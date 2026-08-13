package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.ScopusFuente;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.ScopusFuenteRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;

/** Strategy del Source Title List. Nunca publica título, editorial ni métricas. */
@Component
public class ScopusCatalogProvider implements JournalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ScopusCatalogProvider.class);
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /*
     * El Source Title List oficial contiene una parte XML descomprimida de unos
     * 110 MB. POI limita por defecto cada byte[] a 100 MB. El margen de 150 MB
     * sólo se habilita durante WorkbookFactory.create y se restaura enseguida,
     * para no cambiar el límite usado por los importadores de Springer/DOAJ.
     */
    private static final int SCOPUS_POI_MAX_BYTE_ARRAY = 150_000_000;
    private static final Object POI_OPEN_LOCK = new Object();
    private static final Pattern LINK = Pattern.compile(
            "(?:https?:)?//downloads\\.ctfassets\\.net/[^\"'<>\\s]+\\.xlsx",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> ISSN = Set.of("issn", "print issn", "print-issn", "p issn", "p-issn");
    private static final Set<String> EISSN = Set.of("eissn", "e-issn", "electronic issn", "online issn");
    private static final Set<String> STATUS = Set.of("status", "source status", "active or inactive", "active/inactive");
    private static final Set<String> DISCONTINUED = Set.of(
            "titles discontinued by scopus", "title discontinued by scopus",
            "indexation change");
    private static final Set<String> FREQUENCY = Set.of(
            "frequency", "publication frequency", "publishing frequency", "periodicity", "periodicidad");

    private final ScopusFuenteRepository repository;
    private final AuditoriaService auditoria;
    private final HttpClient http;
    private final URI officialPage;
    private final Duration timeout;
    private final Path hashPath;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private TransactionTemplate transactionTemplate;

    public ScopusCatalogProvider(
            ScopusFuenteRepository repository, AuditoriaService auditoria,
            @Value("${scopus.catalog.url}") String url,
            @Value("${scopus.catalog.connect-timeout-ms:10000}") long connectMs,
            @Value("${scopus.catalog.read-timeout-ms:120000}") long readMs,
            @Value("${scopus.catalog.hash-file:data/scopus-source-list.sha256}") String hashFile,
            @Value("${scopus.catalog.enabled:true}") boolean enabled) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        this.officialPage = URI.create(url);
        this.timeout = Duration.ofMillis(readMs);
        this.hashPath = Path.of(hashFile);
        this.enabled = enabled;
    }

    @Autowired
    void configurarTransacciones(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override public String nombre() { return "scopus_excel"; }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        return find(normalizeIssn(issn), normalizeIssn(eissn)).map(source -> {
            Map<String, String> data = new LinkedHashMap<>();
            put(data, "estado", source.getEstado());
            put(data, "discontinuada", source.isDiscontinuada() ? "Sí" : "No");
            put(data, "periodicidad", source.getPeriodicidad());
            return new JournalInfo(nombre(), null, source.getIssn(), source.getEissn(),
                    null, null, null, null, null, null, null, null, data);
        });
    }

    public CatalogImportResponseDTO importar(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid(
                "Debe proporcionar el Source Title List de Scopus en Excel");
        long start = System.nanoTime();
        int read = 0, inserted = 0, updated = 0, rejected = 0;
        LOG.info("Scopus Excel [ETL]: comienza lectura de {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());
        try (Workbook workbook = openScopusWorkbook(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            boolean compatible = false;
            for (Sheet sheet : workbook) {
                if (!normalizeHeader(sheet.getSheetName()).startsWith("scopus sources")) {
                    continue;
                }
                Header header = header(sheet, formatter, evaluator);
                if (header == null) {
                    LOG.debug("Scopus Excel [ETL]: hoja '{}' omitida; sin encabezados compatibles",
                            sheet.getSheetName());
                    continue;
                }
                LOG.info("Scopus Excel [ETL]: hoja '{}' compatible; encabezado en fila {}",
                        sheet.getSheetName(), header.row() + 1);
                compatible = true;
                repository.deleteAllInBatch();
                for (int i = header.row() + 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String issn = value(row, header.issn(), formatter, evaluator);
                    String eissn = value(row, header.eissn(), formatter, evaluator);
                    if (issn == null && eissn == null) continue;
                    read++;
                    if (normalizeIssn(issn) == null && normalizeIssn(eissn) == null) {
                        rejected++;
                        LOG.warn("Scopus Excel: fila {} rechazada: ISSN/eISSN inválido", i + 1);
                        continue;
                    }
                    boolean isNew = save(issn, eissn,
                            value(row, header.status(), formatter, evaluator),
                            header.discontinued() == null ? null : discontinued(
                                    value(row, header.discontinued(), formatter, evaluator)),
                            value(row, header.frequency(), formatter, evaluator));
                    if (isNew) inserted++; else updated++;
                }
                repository.flush();
                break;
            }
            if (!compatible) throw invalid(
                    "El Excel no contiene la hoja principal Scopus Sources con encabezados ISSN/eISSN reconocibles");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("No fue posible leer el Excel de Scopus: " + message(ex));
        }
        LOG.info("Scopus Excel [persistencia]: ETL finalizado; leídos={}, insertados={}, actualizados={}, rechazados={}",
                read, inserted, updated, rejected);
        return new CatalogImportResponseDTO(read, inserted, updated, rejected,
                (System.nanoTime() - start) / 1_000_000);
    }

    /** Mensual; el scheduler global de un hilo impide descargas paralelas. */
    @Scheduled(cron = "${scopus.catalog.cron:0 0 3 1 * *}")
    public void descargarEImportar() {
        descargarEImportar("scheduler mensual");
    }

    private void descargarEImportar(String origen) {
        LOG.info("Scopus Excel [scheduler]: ejecución solicitada por {}", origen);
        if (!enabled) {
            LOG.info("Scopus Excel [scheduler]: ejecución omitida; descarga deshabilitada");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            LOG.info("Scopus Excel [scheduler]: ejecución omitida; ya existe otra carga en curso");
            return;
        }
        try {
            LOG.info("Scopus Excel [descarga]: consultando {}", officialPage);
            byte[] excel = downloadExcel();
            LOG.info("Scopus Excel [descarga]: XLSX validado; {} bytes recibidos", excel.length);
            String hash = CatalogDownloadSupport.calcularSha256(excel);
            String estadoEsperado = hash + ":" + repository.count();
            if (estadoEsperado.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                audit(true, "IMPORTACIÓN_AUTOMÁTICA", "Source Title List sin cambios; importación omitida. SHA-256: " + hash);
                return;
            }
            LOG.info("Scopus Excel [ETL]: inicia importación automática; SHA-256={}", hash);
            InMemoryMultipartFile archivo = new InMemoryMultipartFile(
                    excel, "scopus-source-title-list.xlsx", XLSX);
            CatalogImportResponseDTO resultado = transactionTemplate == null
                    ? importar(archivo)
                    : transactionTemplate.execute(status -> importar(archivo));
            if (resultado == null) {
                throw new IllegalStateException("La transacción Scopus no devolvió resultado");
            }
            long totalPersistido = repository.count();
            CatalogDownloadSupport.guardarHash(hashPath, hash + ":" + totalPersistido,
                    "scopus-source-list-");
            LOG.info("Scopus Excel [persistencia]: carga completada; insertados={}, actualizados={}, rechazados={}, totalTabla={}",
                    resultado.getRegistrosInsertados(), resultado.getRegistrosActualizados(),
                    resultado.getRegistrosRechazados(), totalPersistido);
            audit(true, "IMPORTACIÓN_AUTOMÁTICA", "Source Title List descargado e importado. SHA-256: " + hash);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            audit(false, "IMPORTACIÓN_AUTOMÁTICA", "Descarga interrumpida");
        } catch (Exception ex) {
            LOG.error("Falló la descarga/importación del Source Title List", ex);
            audit(false, "IMPORTACIÓN_AUTOMÁTICA",
                    "Falló la descarga o importación del Source Title List: " + message(ex));
        } finally {
            running.set(false);
        }
    }

    private byte[] downloadExcel() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = get(officialPage, "text/html," + XLSX);
        checkHttp(response, "Elsevier");
        LOG.info("Scopus Excel [descarga]: respuesta inicial HTTP {}; {} bytes",
                response.statusCode(), response.body() == null ? 0 : response.body().length);
        if (isXlsx(response.body())) return checkExcel(response.body());
        String html = new String(response.body(), StandardCharsets.UTF_8)
                .replace("\\u002F", "/").replace("\\/", "/");
        Matcher matcher = LINK.matcher(html);
        if (!matcher.find()) throw new IOException("Elsevier cambió el formato o el enlace del Source Title List");
        String enlace = matcher.group();
        URI excelUri = URI.create(enlace.startsWith("//") ? "https:" + enlace : enlace);
        LOG.info("Scopus Excel [descarga]: enlace oficial localizado: {}", excelUri);
        response = get(excelUri, XLSX);
        checkHttp(response, "descarga XLSX de Elsevier");
        LOG.info("Scopus Excel [descarga]: respuesta XLSX HTTP {}; {} bytes",
                response.statusCode(), response.body() == null ? 0 : response.body().length);
        return checkExcel(response.body());
    }

    private HttpResponse<byte[]> get(URI uri, String accept) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).header("Accept", accept)
                .header("User-Agent", "CRECUTEQ-Scopus-Source-List/1.0").GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private void checkHttp(HttpResponse<?> response, String origin) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException(origin + " respondió HTTP " + response.statusCode());
    }
    private byte[] checkExcel(byte[] bytes) throws IOException {
        if (!isXlsx(bytes)) throw new IOException("La respuesta no es un XLSX válido");
        try (Workbook ignored = openScopusWorkbook(new ByteArrayInputStream(bytes))) {
            return bytes;
        } catch (RuntimeException ex) {
            throw new IOException("El XLSX de Scopus no puede abrirse", ex);
        }
    }

    /**
     * Aplica de forma acotada el override recomendado por Apache POI para partes
     * OOXML grandes. El bloqueo evita que dos aperturas Scopus alteren entre sí
     * el valor global y el finally conserva la configuración previa de POI.
     */
    private Workbook openScopusWorkbook(InputStream input) throws IOException {
        synchronized (POI_OPEN_LOCK) {
            int previousLimit = IOUtils.getByteArrayMaxOverride();
            try {
                if (previousLimit < SCOPUS_POI_MAX_BYTE_ARRAY) {
                    IOUtils.setByteArrayMaxOverride(SCOPUS_POI_MAX_BYTE_ARRAY);
                }
                return WorkbookFactory.create(input);
            } finally {
                IOUtils.setByteArrayMaxOverride(previousLimit);
            }
        }
    }
    private boolean isXlsx(byte[] bytes) {
        return bytes != null && bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private Header header(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Row row : sheet) {
            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : row) {
                String name = normalizeHeader(formatter.formatCellValue(cell, evaluator));
                if (ISSN.contains(name)) columns.putIfAbsent("issn", cell.getColumnIndex());
                if (EISSN.contains(name)) columns.putIfAbsent("eissn", cell.getColumnIndex());
                if (STATUS.contains(name)) columns.putIfAbsent("status", cell.getColumnIndex());
                if (DISCONTINUED.contains(name)) columns.putIfAbsent("discontinued", cell.getColumnIndex());
                if (FREQUENCY.contains(name)) columns.putIfAbsent("frequency", cell.getColumnIndex());
            }
            if (columns.containsKey("issn") || columns.containsKey("eissn"))
                return new Header(row.getRowNum(), columns.get("issn"), columns.get("eissn"),
                        columns.get("status"), columns.get("discontinued"),
                        columns.get("frequency"));
        }
        return null;
    }

    private boolean save(
            String issn, String eissn, String status, Boolean discontinued,
            String frequency
    ) {
        String print = normalizeIssn(issn), electronic = normalizeIssn(eissn);
        if (print == null && electronic == null) throw new IllegalArgumentException("ISSN/eISSN inválido");
        ScopusFuente source = new ScopusFuente();
        source.setIssn(format(print)); source.setIssnNormalizado(print);
        source.setEissn(format(electronic)); source.setEissnNormalizado(electronic);
        String sourceStatus = clean(status);
        if (sourceStatus != null) source.setEstado(sourceStatus);
        if (discontinued != null) source.setDiscontinuada(discontinued);
        // Sólo una columna explícita: nunca se inventa o estima periodicidad.
        String sourceFrequency = clean(frequency);
        if (sourceFrequency != null) source.setPeriodicidad(sourceFrequency);
        repository.save(source);
        return true;
    }

    private Optional<ScopusFuente> find(String issn, String eissn) {
        Optional<ScopusFuente> result = Optional.empty();
        if (issn != null) {
            result = repository.findFirstByIssnNormalizado(issn);
        }
        if (result.isEmpty() && eissn != null) {
            result = repository.findFirstByEissnNormalizado(eissn);
        }
        return result;
    }

    private boolean discontinued(String value) {
        return clean(value) != null;
    }
    private String value(Row row, Integer column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (column == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : clean(formatter.formatCellValue(cell, evaluator));
    }
    private String normalizeHeader(String value) {
        value = clean(value);
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('\u00a0', ' ').replaceAll("\\s+", " ");
    }
    private String normalizeIssn(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        return normalized.matches("\\d{7}[\\dX]") ? normalized : null;
    }
    private String format(String value) { return value == null ? null : value.substring(0, 4) + "-" + value.substring(4); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void put(Map<String, String> map, String key, String value) { if (value != null) map.put(key, value); }
    private String message(Exception ex) { return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(); }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void audit(boolean success, String action, String description) {
        try {
            if (success) auditoria.registrarExito(null, "SCOPUS_EXCEL", action, description);
            else auditoria.registrarError(null, "SCOPUS_EXCEL", action, description);
        } catch (RuntimeException ex) {
            LOG.error("No fue posible registrar la auditoría Scopus Excel", ex);
        }
    }
    private record Header(
            int row, Integer issn, Integer eissn, Integer status,
            Integer discontinued, Integer frequency
    ) {}
}
