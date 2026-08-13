package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.WileyRevista;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.WileyRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Strategy unificada para las listas Open Access y OnlineOpen de Wiley. */
@Component
public class WileyProvider implements JournalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(WileyProvider.class);
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final WileyRevistaRepository repository;
    private final AuditoriaService auditoria;
    private final HttpClient http;
    private final URI openAccessUrl;
    private final URI hybridUrl;
    private final Duration timeout;
    private final Path openAccessHash;
    private final Path hybridHash;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private TransactionTemplate transactionTemplate;

    @Autowired
    public WileyProvider(WileyRevistaRepository repository, AuditoriaService auditoria,
            @Value("${wiley.pricing.open-access-url}") String openAccessUrl,
            @Value("${wiley.pricing.hybrid-url}") String hybridUrl,
            @Value("${wiley.pricing.connect-timeout-ms:10000}") long connectMs,
            @Value("${wiley.pricing.read-timeout-ms:120000}") long readMs,
            @Value("${wiley.pricing.open-access-hash-file:data/wiley-open-access.sha256}") String openHash,
            @Value("${wiley.pricing.hybrid-hash-file:data/wiley-hybrid.sha256}") String hybridHash,
            @Value("${wiley.pricing.enabled:true}") boolean enabled) {
        this(repository, auditoria, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs))
                        .followRedirects(HttpClient.Redirect.NORMAL).build(),
                URI.create(openAccessUrl), URI.create(hybridUrl), Duration.ofMillis(readMs),
                Path.of(openHash), Path.of(hybridHash), enabled);
    }

    WileyProvider(WileyRevistaRepository repository, AuditoriaService auditoria, HttpClient http,
            URI openAccessUrl, URI hybridUrl, Duration timeout, Path openAccessHash,
            Path hybridHash, boolean enabled) {
        this.repository = repository; this.auditoria = auditoria; this.http = http;
        this.openAccessUrl = openAccessUrl; this.hybridUrl = hybridUrl; this.timeout = timeout;
        this.openAccessHash = openAccessHash; this.hybridHash = hybridHash; this.enabled = enabled;
    }

    @Autowired
    void configurarTransacciones(PlatformTransactionManager manager) {
        transactionTemplate = new TransactionTemplate(manager);
    }

    @Override public String nombre() { return "wiley"; }

    @Override @Transactional(readOnly = true)
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        Optional<WileyRevista> resultado = buscar(normalizarIssn(eissn));
        if (resultado.isEmpty()) resultado = buscar(normalizarIssn(issn));
        return resultado.map(this::convertir);
    }

    private Optional<WileyRevista> buscar(String issn) {
        return issn == null ? Optional.empty() : repository.findFirstByOnlineIssnNormalizado(issn);
    }

    @Transactional
    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        return importarArchivo(archivo);
    }

    private CatalogImportResponseDTO importarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw invalido("Debe proporcionar una lista XLSX oficial de Wiley");
        long inicio = System.nanoTime();
        int leidos = 0, insertados = 0, actualizados = 0, rechazados = 0;
        try (Workbook libro = WorkbookFactory.create(archivo.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = libro.getCreationHelper().createFormulaEvaluator();
            Sheet hoja = libro.getSheetAt(0);
            Header header = encontrarHeader(hoja, formatter, evaluator);
            if (header == null) throw invalido("El Excel no contiene los encabezados oficiales de Wiley");
            String modelo = header.hibrido() ? "Hybrid Open Access (OnlineOpen)" : "Fully Open Access";
            String vigencia = encontrarVigencia(hoja, header.fila(), formatter, evaluator);
            for (int i = header.fila() + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i); if (row == null) continue;
                String titulo = valor(row, header.titulo(), formatter, evaluator);
                String issn = valor(row, header.issn(), formatter, evaluator);
                if (titulo == null && issn == null) continue;
                leidos++;
                String normalizado = normalizarIssn(issn);
                if (titulo == null || normalizado == null) { rechazados++; continue; }
                Optional<WileyRevista> existente = repository.findFirstByOnlineIssnNormalizado(normalizado);
                WileyRevista revista = existente.orElseGet(WileyRevista::new);
                revista.setTitulo(titulo); revista.setOnlineIssn(formatearIssn(normalizado));
                revista.setOnlineIssnNormalizado(normalizado); revista.setModeloPublicacion(modelo);
                revista.setAreaTematica(valor(row, header.area(), formatter, evaluator));
                revista.setLicencias(valor(row, header.licencias(), formatter, evaluator));
                revista.setApcUsd(valor(row, header.usd(), formatter, evaluator));
                revista.setApcGbp(valor(row, header.gbp(), formatter, evaluator));
                revista.setApcEur(valor(row, header.eur(), formatter, evaluator));
                revista.setVigencia(vigencia); revista.setFuenteArchivo(archivo.getOriginalFilename());
                repository.save(revista);
                if (existente.isEmpty()) insertados++; else actualizados++;
            }
            repository.flush();
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw invalido("No fue posible leer el Excel de Wiley: " + mensaje(ex)); }
        return new CatalogImportResponseDTO(leidos, insertados, actualizados, rechazados,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    @Scheduled(cron = "${wiley.pricing.cron:0 15 2 1 * *}")
    public void descargarEImportar() {
        if (!enabled || !running.compareAndSet(false, true)) return;
        try {
            procesarCatalogo(openAccessUrl, openAccessHash, "wiley-open-access.xlsx", "OPEN_ACCESS");
            procesarCatalogo(hybridUrl, hybridHash, "wiley-online-open.xlsx", "HYBRID");
        } finally { running.set(false); }
    }

    private void procesarCatalogo(URI url, Path hashPath, String nombreArchivo, String tipo) {
        try {
            byte[] excel = descargar(url);
            String hash = CatalogDownloadSupport.calcularSha256(excel);
            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                auditar(true, tipo, "Lista sin cambios; importación omitida. SHA-256: " + hash);
                return;
            }
            InMemoryMultipartFile archivo = new InMemoryMultipartFile(excel, nombreArchivo, XLSX);
            CatalogImportResponseDTO resultado = transactionTemplate == null
                    ? importarArchivo(archivo)
                    : transactionTemplate.execute(status -> importarArchivo(archivo));
            if (resultado == null) throw new IllegalStateException("La transacción Wiley no devolvió resultado");
            CatalogDownloadSupport.guardarHash(hashPath, hash, "wiley-" + tipo.toLowerCase(Locale.ROOT) + "-");
            auditar(true, tipo, "Lista descargada e importada: " + resultado.getRegistrosLeidos()
                    + " registros leídos. SHA-256: " + hash);
        } catch (Exception ex) {
            LOG.error("Falló la descarga/importación Wiley {}", tipo, ex);
            auditar(false, tipo, "Falló la descarga/importación: " + mensaje(ex));
        }
    }

    private byte[] descargar(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(url).timeout(timeout).header("Accept", XLSX)
                .header("User-Agent", "CRECUTEQ-Wiley-APC/1.0").GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Wiley respondió con HTTP " + response.statusCode());
        byte[] body = response.body();
        if (body == null || body.length < 4 || body[0] != 'P' || body[1] != 'K')
            throw new IOException("Wiley no devolvió un XLSX válido");
        return body;
    }

    private Header encontrarHeader(Sheet hoja, DataFormatter f, FormulaEvaluator e) {
        for (int i = hoja.getFirstRowNum(); i <= Math.min(hoja.getLastRowNum(), 30); i++) {
            Row row = hoja.getRow(i); if (row == null) continue;
            Map<String, Integer> c = new HashMap<>();
            for (Cell cell : row) c.put(normalizarTexto(f.formatCellValue(cell, e)), cell.getColumnIndex());
            Integer titulo = primera(c, "journal name", "journal title");
            Integer issn = primera(c, "online issn");
            Integer area = primera(c, "subject area");
            Integer licencias = primera(c, "license types offered");
            Integer usd = moneda(c, "usd"), gbp = moneda(c, "gbp"), eur = moneda(c, "eur");
            if (titulo != null && issn != null && area != null && licencias != null
                    && usd != null && gbp != null && eur != null)
                return new Header(i, titulo, area, issn, licencias, usd, gbp, eur,
                        c.containsKey("journal title"));
        }
        return null;
    }

    private Integer moneda(Map<String, Integer> columnas, String prefijo) {
        return columnas.entrySet().stream().filter(e -> e.getKey().startsWith(prefijo))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }
    private Integer primera(Map<String, Integer> c, String... nombres) {
        for (String n : nombres) if (c.containsKey(n)) return c.get(n); return null;
    }
    private String encontrarVigencia(Sheet hoja, int hasta, DataFormatter f, FormulaEvaluator e) {
        for (int i = hoja.getFirstRowNum(); i < hasta; i++) { Row row = hoja.getRow(i); if (row == null) continue;
            for (Cell cell : row) { String v = limpiar(f.formatCellValue(cell, e));
                if (v != null && normalizarTexto(v).startsWith("updated:")) return v.substring(v.indexOf(':') + 1).trim(); } }
        return null;
    }
    private JournalInfo convertir(WileyRevista r) {
        Map<String, String> d = new LinkedHashMap<>();
        poner(d, "modeloPublicacion", r.getModeloPublicacion()); poner(d, "areaTematica", r.getAreaTematica());
        poner(d, "licencias", r.getLicencias()); poner(d, "apcUsd", r.getApcUsd());
        poner(d, "apcGbp", r.getApcGbp()); poner(d, "apcEur", r.getApcEur()); poner(d, "vigencia", r.getVigencia());
        poner(d, "openAccess", "Sí");
        return new JournalInfo(nombre(), r.getTitulo(), null, r.getOnlineIssn(), "Wiley", null, null,
                r.getAreaTematica(), r.getLicencias(), r.getApcUsd(), r.getApcUsd() == null ? null : "USD",
                "https://authors.wiley.com/author-resources/Journal-Authors/open-access/article-publication-charges/index.html", d);
    }
    private String valor(Row r, int col, DataFormatter f, FormulaEvaluator e) { Cell c = r.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL); return c == null ? null : limpiar(f.formatCellValue(c, e)); }
    private String normalizarIssn(String v) { if (v == null) return null; String n = v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]", ""); return n.matches("\\d{7}[\\dX]") ? n : null; }
    private String formatearIssn(String n) { return n.substring(0, 4) + "-" + n.substring(4); }
    private String normalizarTexto(String v) { return v == null ? "" : v.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    private String limpiar(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String mensaje(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private ResponseStatusException invalido(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private void poner(Map<String, String> d, String k, String v) { if (v != null) d.put(k, v); }
    private void auditar(boolean exito, String tipo, String descripcion) {
        try { String accion = "IMPORTACIÓN_APC_AUTOMÁTICA_" + tipo;
            if (exito) auditoria.registrarExito(null, "WILEY", accion, descripcion);
            else auditoria.registrarError(null, "WILEY", accion, descripcion);
        } catch (RuntimeException ex) { LOG.error("No se pudo registrar la auditoría Wiley", ex); }
    }
    private record Header(int fila, int titulo, int area, int issn, int licencias,
                          int usd, int gbp, int eur, boolean hibrido) { }
}
