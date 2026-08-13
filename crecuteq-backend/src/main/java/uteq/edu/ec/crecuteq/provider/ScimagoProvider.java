package uteq.edu.ec.crecuteq.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.Scimago;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.CsvRecordReader;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.ScimagoRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** ETL del catálogo SCImago. No expone revistas como resultados independientes. */
@Component
public class ScimagoProvider implements JournalProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScimagoProvider.class);
    private final ScimagoRepository repository;
    private final AuditoriaService auditoriaService;
    private final HttpClient httpClient;
    private final URI catalogoUrl;
    private final Duration readTimeout;
    private final Path hashPath;
    private final boolean descargaHabilitada;
    private final AtomicBoolean descargando = new AtomicBoolean(false);

    @Autowired
    public ScimagoProvider(
            ScimagoRepository repository,
            AuditoriaService auditoriaService,
            @Value("${scimago.download.url:https://www.scimagojr.com/journalrank.php?out=xls}") String url,
            @Value("${scimago.download.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${scimago.download.read-timeout-ms:180000}") long readTimeoutMs,
            @Value("${scimago.download.hash-file:data/scimago-catalog.sha256}") String hashFile,
            @Value("${scimago.download.enabled:true}") boolean habilitada
    ) {
        this(repository, auditoriaService,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .followRedirects(HttpClient.Redirect.NORMAL).build(),
                URI.create(url), Duration.ofMillis(readTimeoutMs), Path.of(hashFile), habilitada);
    }

    ScimagoProvider(ScimagoRepository repository, AuditoriaService auditoriaService,
                     HttpClient httpClient, URI catalogoUrl, Duration readTimeout,
                     Path hashPath, boolean descargaHabilitada) {
        this.repository = repository;
        this.auditoriaService = auditoriaService;
        this.httpClient = httpClient;
        this.catalogoUrl = catalogoUrl;
        this.readTimeout = readTimeout;
        this.hashPath = hashPath;
        this.descargaHabilitada = descargaHabilitada;
    }

    @Override
    public String nombre() { return "scimago"; }

    /** SCImago se consulta exclusivamente desde RevistaService para enriquecer Scopus. */
    @Override
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        return Optional.empty();
    }

    @Scheduled(cron = "${scimago.download.cron:0 45 1 1 * *}")
    public void descargarEImportar() {
        if (!descargaHabilitada || !descargando.compareAndSet(false, true)) return;
        try {
            byte[] archivo = descargarCsv();
            String hash = CatalogDownloadSupport.calcularSha256(archivo);
            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                auditarExito("Catálogo oficial sin cambios; importación omitida. SHA-256: " + hash);
                return;
            }
            importarInterno(new InMemoryMultipartFile(archivo, "scimago.csv", "text/csv"), false);
            CatalogDownloadSupport.guardarHash(hashPath, hash, "scimago-catalog-");
            auditarExito("Catálogo oficial descargado e importado. SHA-256: " + hash);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            auditarError("Descarga interrumpida");
        } catch (Exception exception) {
            LOGGER.error("No fue posible descargar o importar SCImago", exception);
            auditarError("No fue posible descargar o importar el catálogo oficial: " + mensaje(exception));
        } finally {
            descargando.set(false);
        }
    }

    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        return importarInterno(archivo, false);
    }

    private CatalogImportResponseDTO importarInterno(MultipartFile archivo, boolean manual) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe proporcionar el CSV oficial de SCImago");
        }
        long inicio = System.nanoTime();
        int leidos = 0, insertados = 0, actualizados = 0, rechazados = 0;
        try {
            byte[] contenido = archivo.getBytes();
            char separador = detectarSeparador(contenido);
            Map<Double, Scimago> existentes = new HashMap<>();
            repository.findAll().forEach(s -> existentes.put(s.getSourceid(), s));
            List<Scimago> cambios = new ArrayList<>();
            try (var csv = new CsvRecordReader(new InputStreamReader(
                    new ByteArrayInputStream(contenido), StandardCharsets.UTF_8), separador)) {
                List<String> encabezados = csv.leerRegistro();
                validarEncabezados(encabezados);
                List<String> valores;
                while ((valores = csv.leerRegistro()) != null) {
                    if (valores.stream().allMatch(String::isBlank)) continue;
                    leidos++;
                    try {
                        Map<String, String> fila = convertirFila(encabezados, valores);
                        Double id = numero(campo(fila, "Sourceid", "Source ID"));
                        if (id == null) throw new IllegalArgumentException("Sourceid inválido");
                        Scimago entidad = existentes.get(id);
                        boolean nueva = entidad == null;
                        if (nueva) entidad = new Scimago();
                        entidad.setSourceid(id);
                        entidad.setTitle(campo(fila, "Title"));
                        entidad.setIssn(campo(fila, "Issn", "ISSN"));
                        entidad.setCountry(campo(fila, "Country"));
                        entidad.setSJRBestQuartile(campo(fila, "SJR Best Quartile"));
                        entidad.setCoverage(campo(fila, "Coverage"));
                        entidad.setType(campo(fila, "Type"));
                        entidad.setSjr(campo(fila, "SJR"));
                        entidad.setRegion(campo(fila, "Region"));
                        entidad.setCategories(campo(fila, "Categories"));
                        entidad.setAreas(campo(fila, "Areas"));
                        cambios.add(entidad);
                        existentes.put(id, entidad);
                        if (nueva) insertados++; else actualizados++;
                    } catch (RuntimeException exception) {
                        rechazados++;
                        LOGGER.warn("Importación SCImago: fila {} rechazada: {}", leidos + 1,
                                exception.getMessage());
                    }
                }
            }
            // Una única operación transaccional de repositorio: no se borra ningún registro previo.
            repository.saveAllAndFlush(cambios);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No fue posible leer el CSV de SCImago: " + mensaje(exception), exception);
        }
        return new CatalogImportResponseDTO(leidos, insertados, actualizados, rechazados,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    private byte[] descargarCsv() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(catalogoUrl).timeout(readTimeout)
                .header("Accept", "text/csv,application/vnd.ms-excel,*/*")
                .header("User-Agent", "Mozilla/5.0 CRECUTEQ-SCImago-ETL/1.0")
                .header("Referer", "https://www.scimagojr.com/journalrank.php")
                .GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("SCImago respondió con código HTTP " + response.statusCode());
        if (response.body() == null || response.body().length == 0)
            throw new IOException("SCImago respondió con un archivo vacío");
        return response.body();
    }

    private char detectarSeparador(byte[] contenido) {
        String primera = new String(contenido, 0, Math.min(contenido.length, 4096), StandardCharsets.UTF_8)
                .lines().findFirst().orElse("");
        return primera.chars().filter(c -> c == ';').count()
                > primera.chars().filter(c -> c == ',').count() ? ';' : ',';
    }

    private void validarEncabezados(List<String> encabezados) {
        if (encabezados == null || encabezados.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CSV está vacío");
        encabezados.set(0, encabezados.get(0).replace("\uFEFF", ""));
        if (!contiene(encabezados, "Sourceid") || !contiene(encabezados, "Title")
                || !contiene(encabezados, "Issn") || !contiene(encabezados, "Country")
                || !contiene(encabezados, "SJR Best Quartile"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo no tiene los encabezados oficiales de SCImago");
    }

    private boolean contiene(List<String> lista, String nombre) {
        return lista.stream().anyMatch(v -> normalizar(v).equals(normalizar(nombre)));
    }
    private Map<String, String> convertirFila(List<String> h, List<String> v) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < h.size(); i++) fila.put(h.get(i).trim(), i < v.size() ? v.get(i).trim() : "");
        return fila;
    }
    private String campo(Map<String, String> fila, String... nombres) {
        for (String nombre : nombres) for (var e : fila.entrySet())
            if (normalizar(e.getKey()).equals(normalizar(nombre)) && !e.getValue().isBlank()) return e.getValue().trim();
        return null;
    }
    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
    private Double numero(String valor) {
        if (valor == null) return null;
        try { return Double.valueOf(valor.replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }
    private void auditarExito(String texto) {
        try { auditoriaService.registrarExito(null, "SCIMAGO", "IMPORTACIÓN_AUTOMÁTICA", texto); }
        catch (RuntimeException e) { LOGGER.error("No fue posible auditar SCImago", e); }
    }
    private void auditarError(String texto) {
        try { auditoriaService.registrarError(null, "SCIMAGO", "IMPORTACIÓN_AUTOMÁTICA", texto); }
        catch (RuntimeException e) { LOGGER.error("No fue posible auditar SCImago", e); }
    }
    private String mensaje(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
