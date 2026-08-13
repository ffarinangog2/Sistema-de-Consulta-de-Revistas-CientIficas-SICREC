package uteq.edu.ec.crecuteq.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.entity.DoajRevista;
import uteq.edu.ec.crecuteq.etl.CsvRecordReader;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.DoajRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

// INICIO - Estrategia DOAJ
@Component
public class DoajProvider implements JournalProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoajProvider.class);
    private final DoajRevistaRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditoriaService auditoriaService;
    // INICIO - Descarga automática del catálogo DOAJ
    private final HttpClient httpClient;
    private final URI catalogoUrl;
    private final Duration readTimeout;
    private final Path hashPath;
    private final boolean descargaHabilitada;
    private final AtomicBoolean descargando = new AtomicBoolean(false);
    // FIN - Descarga automática del catálogo DOAJ

    @Autowired
    public DoajProvider(
            DoajRevistaRepository repository,
            AuditoriaService auditoriaService,
            @Value("${doaj.download.url:https://doaj.org/csv}") String catalogoUrl,
            @Value("${doaj.download.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${doaj.download.read-timeout-ms:120000}") long readTimeoutMs,
            @Value("${doaj.download.hash-file:data/doaj-catalog.sha256}") String hashFile,
            @Value("${doaj.download.enabled:true}") boolean descargaHabilitada
    ) {
        this(repository, auditoriaService,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                URI.create(catalogoUrl), Duration.ofMillis(readTimeoutMs),
                Path.of(hashFile), descargaHabilitada);
    }

    DoajProvider(
            DoajRevistaRepository repository,
            AuditoriaService auditoriaService,
            HttpClient httpClient,
            URI catalogoUrl,
            Duration readTimeout,
            Path hashPath,
            boolean descargaHabilitada
    ) {
        this.repository = repository;
        this.auditoriaService = auditoriaService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
        this.catalogoUrl = catalogoUrl;
        this.readTimeout = readTimeout;
        this.hashPath = hashPath;
        this.descargaHabilitada = descargaHabilitada;
    }

    @Override
    public String nombre() {
        return "doaj";
    }

    // INICIO - Descarga automática DOAJ con detección SHA-256
    @Scheduled(cron = "${doaj.download.cron:0 15 1 1 * *}")
    public void descargarEImportar() {
        if (!descargaHabilitada || !descargando.compareAndSet(false, true)) {
            return;
        }
        try {
            byte[] archivo = descargarCsv();
            String hash = CatalogDownloadSupport.calcularSha256(archivo);
            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                registrarExitoAutomatico(
                        "Catálogo oficial sin cambios; importación omitida. SHA-256: "
                                + hash);
                return;
            }

            importar(new InMemoryMultipartFile(
                    archivo, "doaj-journal-list.csv", "text/csv"));
            CatalogDownloadSupport.guardarHash(
                    hashPath, hash, "doaj-catalog-");
            registrarExitoAutomatico(
                    "Catálogo oficial descargado e importado. SHA-256: " + hash);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            registrarErrorAutomatico("Descarga interrumpida");
        } catch (Exception exception) {
            LOGGER.error("No fue posible descargar o importar el catálogo DOAJ",
                    exception);
            registrarErrorAutomatico(
                    "No fue posible descargar o importar el catálogo oficial: "
                            + mensaje(exception));
        } finally {
            descargando.set(false);
        }
    }

    private byte[] descargarCsv() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(catalogoUrl)
                .timeout(readTimeout)
                .header("Accept", "text/csv")
                .header("User-Agent", "CRECUTEQ-DOAJ-Catalog/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "DOAJ respondió con código HTTP " + response.statusCode());
        }
        if (response.body() == null || response.body().length == 0) {
            throw new IOException("DOAJ respondió con un CSV vacío");
        }
        return response.body();
    }

    private void registrarExitoAutomatico(String descripcion) {
        try {
            auditoriaService.registrarExito(
                    null, "DOAJ", "IMPORTACIÓN_AUTOMÁTICA", descripcion);
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible registrar la auditoría DOAJ", exception);
        }
    }

    private void registrarErrorAutomatico(String descripcion) {
        try {
            auditoriaService.registrarError(
                    null, "DOAJ", "IMPORTACIÓN_AUTOMÁTICA", descripcion);
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible registrar la auditoría DOAJ", exception);
        }
    }

    private String mensaje(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
    // FIN - Descarga automática DOAJ con detección SHA-256

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalInfo> buscar(
            String productId, String issn, String eissn
    ) {
        String issnNormalizado = normalizarIssn(issn);
        String eissnNormalizado = normalizarIssn(eissn);
        Optional<DoajRevista> resultado = Optional.empty();

        if (issnNormalizado != null) {
            resultado = repository.findFirstByIssnNormalizado(issnNormalizado);
            if (resultado.isEmpty()) {
                resultado = repository.findFirstByEissnNormalizado(issnNormalizado);
            }
        }
        if (resultado.isEmpty() && eissnNormalizado != null) {
            resultado = repository.findFirstByEissnNormalizado(eissnNormalizado);
            if (resultado.isEmpty()) {
                resultado = repository.findFirstByIssnNormalizado(eissnNormalizado);
            }
        }
        return resultado.map(this::convertir);
    }

    // INICIO - Diferencias del ETL oficial DOAJ encapsuladas en la Strategy
    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe proporcionar el archivo CSV oficial de DOAJ");
        }

        long inicio = System.nanoTime();
        int leidos = 0;
        int insertados = 0;
        int actualizados = 0;
        int rechazados = 0;

        try (var csv = new CsvRecordReader(new InputStreamReader(
                archivo.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> encabezados = csv.leerRegistro();
            validarEncabezados(encabezados);
            List<String> valores;
            while ((valores = csv.leerRegistro()) != null) {
                if (valores.stream().allMatch(String::isBlank)) continue;
                leidos++;
                try {
                    boolean insertado = guardar(
                            convertirFila(encabezados, valores),
                            archivo.getOriginalFilename()
                    );
                    if (insertado) insertados++; else actualizados++;
                } catch (RuntimeException exception) {
                    rechazados++;
                    LOGGER.warn("Importación DOAJ: registro {} rechazado: {}",
                            leidos + 1, exception.getMessage());
                }
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No fue posible leer el CSV de DOAJ: " + exception.getMessage(),
                    exception);
        }

        return new CatalogImportResponseDTO(
                leidos, insertados, actualizados, rechazados,
                (System.nanoTime() - inicio) / 1_000_000
        );
    }

    private boolean guardar(Map<String, String> fila, String fuente) {
        String titulo = campo(fila, "Journal title", "Title");
        String issn = campo(fila, "Journal ISSN (print version)", "ISSN (print)",
                "Print ISSN", "ISSN");
        String eissn = campo(fila, "Journal EISSN (online version)", "EISSN (online)",
                "Online ISSN", "EISSN");
        String issnNormalizado = normalizarIssn(issn);
        String eissnNormalizado = normalizarIssn(eissn);
        if (titulo == null) {
            throw new IllegalArgumentException("La fila DOAJ no contiene Journal title");
        }
        if (issnNormalizado == null && eissnNormalizado == null) {
            throw new IllegalArgumentException(
                    "La fila DOAJ no contiene un ISSN/eISSN válido");
        }

        Optional<DoajRevista> existente = buscarEntidad(
                issnNormalizado, eissnNormalizado);
        DoajRevista revista = existente.orElseGet(DoajRevista::new);
        revista.setTitulo(titulo);
        revista.setIssn(formatearIssn(issnNormalizado));
        revista.setIssnNormalizado(issnNormalizado);
        revista.setEissn(formatearIssn(eissnNormalizado));
        revista.setEissnNormalizado(eissnNormalizado);
        revista.setEditorial(campo(fila, "Publisher"));
        revista.setPais(campo(fila, "Country of publisher", "Country"));
        revista.setIdiomas(campo(fila,
                "Languages in which the journal accepts manuscripts", "Languages"));
        revista.setMaterias(campo(fila,
                "Subject category", "Keywords", "Subjects"));
        revista.setLicencia(campo(fila, "Journal license", "License"));
        revista.setApc(campo(fila,
                "APC amount", "Maximum APC", "Article processing charges (APCs)"));
        revista.setMonedaApc(campo(fila, "APC currency", "Currency"));
        revista.setUrlOficial(campo(fila, "Journal URL", "URL"));
        revista.setFuenteArchivo(fuente);
        try {
            revista.setDatosCsv(objectMapper.writeValueAsString(fila));
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "No se pudieron conservar los datos CSV", exception);
        }
        repository.saveAndFlush(revista);
        return existente.isEmpty();
    }

    private Optional<DoajRevista> buscarEntidad(String issn, String eissn) {
        Optional<DoajRevista> resultado = Optional.empty();
        if (issn != null) {
            resultado = repository.findFirstByIssnNormalizado(issn);
            if (resultado.isEmpty()) {
                resultado = repository.findFirstByEissnNormalizado(issn);
            }
        }
        if (resultado.isEmpty() && eissn != null) {
            resultado = repository.findFirstByEissnNormalizado(eissn);
            if (resultado.isEmpty()) {
                resultado = repository.findFirstByIssnNormalizado(eissn);
            }
        }
        return resultado;
    }

    private void validarEncabezados(List<String> encabezados) {
        if (encabezados == null || encabezados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El CSV está vacío");
        }
        encabezados.set(0, encabezados.get(0).replace("\uFEFF", ""));
        boolean titulo = encabezados.stream().anyMatch(e ->
                e.trim().equalsIgnoreCase("Journal title"));
        boolean identificador = encabezados.stream().anyMatch(e ->
                e.toLowerCase(Locale.ROOT).contains("issn"));
        if (!titulo || !identificador) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo no tiene los encabezados oficiales de DOAJ");
        }
    }

    private Map<String, String> convertirFila(
            List<String> encabezados, List<String> valores
    ) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < encabezados.size(); i++) {
            fila.put(encabezados.get(i).trim(),
                    i < valores.size() ? valores.get(i).trim() : "");
        }
        return fila;
    }

    private String campo(Map<String, String> fila, String... nombres) {
        for (String nombre : nombres) {
            for (var entrada : fila.entrySet()) {
                if (normalizarTexto(entrada.getKey()).equals(normalizarTexto(nombre))) {
                    String valor = limpiar(entrada.getValue());
                    if (valor != null) return valor;
                }
            }
        }
        return null;
    }

    private String normalizarTexto(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ').replaceAll("\\s+", " ");
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String formatearIssn(String valor) {
        return valor == null ? null
                : valor.substring(0, 4) + "-" + valor.substring(4);
    }
    // FIN - Diferencias del ETL oficial DOAJ encapsuladas en la Strategy

    private JournalInfo convertir(DoajRevista revista) {
        Map<String, String> csv = new LinkedHashMap<>();
        try {
            csv.putAll(objectMapper.readValue(
                    revista.getDatosCsv(), new TypeReference<>() { }));
        } catch (Exception ignored) { }
        Map<String, String> datos = new LinkedHashMap<>();
        agregarSiExiste(datos, "periodicidad", campo(csv,
                "Publication frequency", "Publishing frequency",
                "Frequency", "Periodicity"));
        return new JournalInfo(
                nombre(), null, revista.getIssn(), revista.getEissn(),
                null, null, null, null, null, revista.getApc(),
                revista.getMonedaApc(), null, datos
        );
    }

    private void agregarSiExiste(
            Map<String, String> datos, String clave, String valor
    ) {
        if (valor != null) datos.put(clave, valor);
    }

    private String normalizarIssn(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String normalizado = valor.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "");
        return normalizado.matches("\\d{7}[\\dX]") ? normalizado : null;
    }
}
// FIN - Estrategia DOAJ
