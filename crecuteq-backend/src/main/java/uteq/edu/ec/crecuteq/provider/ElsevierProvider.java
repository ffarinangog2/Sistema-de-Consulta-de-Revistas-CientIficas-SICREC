package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.ss.usermodel.*;
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
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.ElsevierRevista;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.ElsevierRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strategy para la lista oficial de Article Publishing Charges de Elsevier. */
@Component
public class ElsevierProvider implements JournalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ElsevierProvider.class);
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Pattern ENLACE_XLSX = Pattern.compile(
            "https://assets\\.ctfassets\\.net/[^\\\"'<>\\s]+/article-publishing-charge\\.xlsx",
            Pattern.CASE_INSENSITIVE);
    private final ElsevierRevistaRepository repository;
    private final AuditoriaService auditoria;
    private final HttpClient http;
    private final URI catalogoUrl;
    private final URI paginaOficial;
    private final Duration timeout;
    private final Path hashPath;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public ElsevierProvider(ElsevierRevistaRepository repository, AuditoriaService auditoria,
            @Value("${elsevier.pricing.source-url:https://www.elsevier.com/about/policies-and-standards/pricing}") String pagina,
            @Value("${elsevier.pricing.download-url}") String url,
            @Value("${elsevier.pricing.connect-timeout-ms:10000}") long connectMs,
            @Value("${elsevier.pricing.read-timeout-ms:120000}") long readMs,
            @Value("${elsevier.pricing.hash-file:data/elsevier-apc.sha256}") String hashFile,
            @Value("${elsevier.pricing.enabled:true}") boolean enabled) {
        this(repository, auditoria, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs))
                        .followRedirects(HttpClient.Redirect.NORMAL).build(),
                URI.create(pagina), URI.create(url), Duration.ofMillis(readMs), Path.of(hashFile), enabled);
    }

    ElsevierProvider(ElsevierRevistaRepository repository, AuditoriaService auditoria,
            HttpClient http, URI catalogoUrl, Duration timeout, Path hashPath, boolean enabled) {
        this(repository, auditoria, http, null, catalogoUrl, timeout, hashPath, enabled);
    }

    ElsevierProvider(ElsevierRevistaRepository repository, AuditoriaService auditoria,
            HttpClient http, URI paginaOficial, URI catalogoUrl, Duration timeout,
            Path hashPath, boolean enabled) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.http = http;
        this.paginaOficial = paginaOficial;
        this.catalogoUrl = catalogoUrl;
        this.timeout = timeout;
        this.hashPath = hashPath;
        this.enabled = enabled;
    }

    @Override public String nombre() { return "elsevier"; }

    @Override @Transactional(readOnly = true)
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        Optional<ElsevierRevista> resultado = buscar(normalizarIssn(issn));
        if (resultado.isEmpty()) resultado = buscar(normalizarIssn(eissn));
        return resultado.map(this::convertir);
    }

    private Optional<ElsevierRevista> buscar(String issn) {
        return issn == null ? Optional.empty() : repository.findFirstByIssnNormalizado(issn);
    }

    @Transactional
    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw invalido("Debe proporcionar el Excel oficial de precios APC de Elsevier");
        long inicio = System.nanoTime();
        int leidos = 0, insertados = 0, actualizados = 0, rechazados = 0;
        try (Workbook libro = WorkbookFactory.create(archivo.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = libro.getCreationHelper().createFormulaEvaluator();
            Sheet hoja = libro.getSheetAt(0);
            Header header = encontrarHeader(hoja, formatter, evaluator);
            if (header == null) throw invalido("El Excel no contiene las columnas oficiales ISSN, Title, Business model, USD, EUR, GBP y JPY");
            String vigencia = encontrarVigencia(hoja, header.fila(), formatter, evaluator);
            for (int fila = header.fila() + 1; fila <= hoja.getLastRowNum(); fila++) {
                Row row = hoja.getRow(fila);
                if (row == null) continue;
                String issn = valor(row, header.issn(), formatter, evaluator);
                String titulo = valor(row, header.titulo(), formatter, evaluator);
                if (issn == null && titulo == null) continue;
                leidos++;
                String normalizado = normalizarIssn(issn);
                if (normalizado == null || titulo == null) { rechazados++; continue; }
                Optional<ElsevierRevista> existente = repository.findFirstByIssnNormalizado(normalizado);
                ElsevierRevista revista = existente.orElseGet(ElsevierRevista::new);
                revista.setIssn(formatearIssn(normalizado));
                revista.setIssnNormalizado(normalizado);
                revista.setTitulo(titulo);
                revista.setModeloPublicacion(valor(row, header.modelo(), formatter, evaluator));
                revista.setApcUsd(valor(row, header.usd(), formatter, evaluator));
                revista.setApcEur(valor(row, header.eur(), formatter, evaluator));
                revista.setApcGbp(valor(row, header.gbp(), formatter, evaluator));
                revista.setApcJpy(valor(row, header.jpy(), formatter, evaluator));
                revista.setVigencia(vigencia);
                revista.setFuenteArchivo(archivo.getOriginalFilename());
                repository.saveAndFlush(revista);
                if (existente.isEmpty()) insertados++; else actualizados++;
            }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw invalido("No fue posible leer el Excel de Elsevier: " + mensaje(ex)); }
        return new CatalogImportResponseDTO(leidos, insertados, actualizados, rechazados,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    @Scheduled(cron = "${elsevier.pricing.cron:0 0 2 1 * *}")
    public void descargarEImportar() {
        if (!enabled || !running.compareAndSet(false, true)) return;
        try {
            byte[] excel = descargar();
            String hash = CatalogDownloadSupport.calcularSha256(excel);
            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                auditar(true, "Lista oficial sin cambios; importación omitida. SHA-256: " + hash);
                return;
            }
            importar(new InMemoryMultipartFile(excel, "elsevier-apc.xlsx", XLSX));
            CatalogDownloadSupport.guardarHash(hashPath, hash, "elsevier-apc-");
            auditar(true, "Lista oficial descargada e importada. SHA-256: " + hash);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            auditar(false, "Descarga interrumpida");
        } catch (Exception ex) {
            LOG.error("No fue posible descargar/importar los APC de Elsevier", ex);
            auditar(false, "Falló la descarga/importación: " + mensaje(ex));
        } finally { running.set(false); }
    }

    private byte[] descargar() throws IOException, InterruptedException {
        URI enlace = descubrirEnlaceVigente();
        HttpRequest request = HttpRequest.newBuilder(enlace).timeout(timeout).header("Accept", XLSX)
                .header("User-Agent", "CRECUTEQ-Elsevier-APC/1.0").GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Elsevier respondió con HTTP " + response.statusCode());
        byte[] body = response.body();
        if (body == null || body.length < 4 || body[0] != 'P' || body[1] != 'K')
            throw new IOException("Elsevier no devolvió un archivo XLSX válido");
        return body;
    }

    private URI descubrirEnlaceVigente() throws IOException, InterruptedException {
        if (paginaOficial == null) return catalogoUrl;
        HttpRequest request = HttpRequest.newBuilder(paginaOficial).timeout(timeout)
                .header("Accept", "text/html")
                .header("User-Agent", "CRECUTEQ-Elsevier-APC/1.0").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.warn("La página de precios Elsevier respondió HTTP {}; se usará el enlace de respaldo",
                    response.statusCode());
            return catalogoUrl;
        }
        Matcher matcher = ENLACE_XLSX.matcher(response.body());
        return matcher.find() ? URI.create(matcher.group()) : catalogoUrl;
    }

    private Header encontrarHeader(Sheet hoja, DataFormatter f, FormulaEvaluator e) {
        for (int i = hoja.getFirstRowNum(); i <= Math.min(hoja.getLastRowNum(), 20); i++) {
            Row row = hoja.getRow(i);
            if (row == null) continue;
            Map<String, Integer> columnas = new HashMap<>();
            for (Cell cell : row) columnas.put(normalizarTexto(f.formatCellValue(cell, e)), cell.getColumnIndex());
            if (columnas.keySet().containsAll(Set.of("issn", "title", "business model", "usd", "eur", "gbp", "jpy")))
                return new Header(i, columnas.get("issn"), columnas.get("title"), columnas.get("business model"),
                        columnas.get("usd"), columnas.get("eur"), columnas.get("gbp"), columnas.get("jpy"));
        }
        return null;
    }

    private String encontrarVigencia(Sheet hoja, int hasta, DataFormatter f, FormulaEvaluator e) {
        for (int i = hoja.getFirstRowNum(); i < hasta; i++) {
            Row row = hoja.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                String v = limpiar(f.formatCellValue(cell, e));
                if (v != null && v.toLowerCase(Locale.ROOT).contains("prices as of date"))
                    return v.substring(v.indexOf(':') + 1).trim();
            }
        }
        return null;
    }

    private JournalInfo convertir(ElsevierRevista r) {
        Map<String, String> datos = new LinkedHashMap<>();
        poner(datos, "modeloPublicacion", r.getModeloPublicacion());
        poner(datos, "apcUsd", r.getApcUsd()); poner(datos, "apcEur", r.getApcEur());
        poner(datos, "apcGbp", r.getApcGbp()); poner(datos, "apcJpy", r.getApcJpy());
        poner(datos, "vigencia", r.getVigencia());
        poner(datos, "openAccess", "Fully open access".equalsIgnoreCase(r.getModeloPublicacion()) ? "Sí" : "No");
        return new JournalInfo(nombre(), r.getTitulo(), r.getIssn(), null, "Elsevier", null, null,
                null, null, r.getApcUsd(), r.getApcUsd() == null ? null : "USD",
                "https://www.elsevier.com/about/policies-and-standards/pricing", datos);
    }

    private String valor(Row row, int columna, DataFormatter f, FormulaEvaluator e) {
        Cell cell = row.getCell(columna, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : limpiar(f.formatCellValue(cell, e));
    }
    private String normalizarIssn(String v) { if (v == null) return null; String n = v.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", ""); return n.matches("\\d{7}[\\dX]") ? n : null; }
    private String formatearIssn(String n) { return n.substring(0, 4) + "-" + n.substring(4); }
    private String normalizarTexto(String v) { return v == null ? "" : v.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    private String limpiar(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String mensaje(Exception ex) { return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(); }
    private ResponseStatusException invalido(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private void poner(Map<String, String> d, String k, String v) { if (v != null) d.put(k, v); }
    private void auditar(boolean exito, String descripcion) {
        try { if (exito) auditoria.registrarExito(null, "ELSEVIER", "IMPORTACIÓN_AUTOMÁTICA", descripcion);
            else auditoria.registrarError(null, "ELSEVIER", "IMPORTACIÓN_AUTOMÁTICA", descripcion);
        } catch (RuntimeException ex) { LOG.error("No se pudo registrar la auditoría Elsevier", ex); }
    }
    private record Header(int fila, int issn, int titulo, int modelo, int usd, int eur, int gbp, int jpy) { }
}
