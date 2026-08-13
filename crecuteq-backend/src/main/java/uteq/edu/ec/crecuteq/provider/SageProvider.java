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
import uteq.edu.ec.crecuteq.entity.SageRevista;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;
import uteq.edu.ec.crecuteq.repository.SageRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Importa las listas oficiales Gold Open Access y Sage Choice. */
@Component
public class SageProvider implements JournalProvider {
    static final String GOLD = "GOLD_OPEN_ACCESS";
    static final String CHOICE = "SAGE_CHOICE";
    private static final Logger LOG = LoggerFactory.getLogger(SageProvider.class);
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*[\\\"']([^\\\"']+\\.xlsx(?:\\?[^\\\"']*)?)[\\\"']", Pattern.CASE_INSENSITIVE);
    private final SageRevistaRepository repository;
    private final AuditoriaService auditoria;
    private final HttpClient http;
    private final URI paginaOficial;
    private final URI goldUrlRespaldo;
    private final URI choiceUrlRespaldo;
    private final Duration timeout;
    private final Path goldHash;
    private final Path choiceHash;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private TransactionTemplate transactionTemplate;

    @Autowired
    public SageProvider(SageRevistaRepository repository, AuditoriaService auditoria,
            @Value("${sage.pricing.source-url}") String paginaOficial,
            @Value("${sage.pricing.gold-url}") String goldUrl,
            @Value("${sage.pricing.choice-url}") String choiceUrl,
            @Value("${sage.pricing.connect-timeout-ms:10000}") long connectMs,
            @Value("${sage.pricing.read-timeout-ms:120000}") long readMs,
            @Value("${sage.pricing.gold-hash-file:data/sage-gold.sha256}") String goldHash,
            @Value("${sage.pricing.choice-hash-file:data/sage-choice.sha256}") String choiceHash,
            @Value("${sage.pricing.enabled:true}") boolean enabled) {
        this(repository, auditoria, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs))
                        .followRedirects(HttpClient.Redirect.NORMAL).build(),
                URI.create(paginaOficial), URI.create(goldUrl), URI.create(choiceUrl), Duration.ofMillis(readMs),
                Path.of(goldHash), Path.of(choiceHash), enabled);
    }

    SageProvider(SageRevistaRepository repository, AuditoriaService auditoria, HttpClient http,
            URI paginaOficial, URI goldUrl, URI choiceUrl, Duration timeout,
            Path goldHash, Path choiceHash, boolean enabled) {
        this.repository = repository; this.auditoria = auditoria; this.http = http;
        this.paginaOficial = paginaOficial; this.goldUrlRespaldo = goldUrl; this.choiceUrlRespaldo = choiceUrl;
        this.timeout = timeout; this.goldHash = goldHash; this.choiceHash = choiceHash; this.enabled = enabled;
    }

    @Autowired void configurarTransacciones(PlatformTransactionManager manager) {
        transactionTemplate = new TransactionTemplate(manager);
    }

    @Override public String nombre() { return "sage"; }

    @Override @Transactional(readOnly = true)
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        String i = normalizarIssn(issn), e = normalizarIssn(eissn);
        Optional<SageRevista> resultado = (i == null && e == null) ? Optional.empty()
                : repository.findFirstByIssnNormalizadoOrEissnNormalizado(i, e);
        if (resultado.isEmpty() && limpiar(productId) != null)
            resultado = repository.findFirstByJournalCodeIgnoreCaseOrTlaIgnoreCase(productId.trim(), productId.trim());
        return resultado.map(this::convertir);
    }

    @Transactional
    public CatalogImportResponseDTO importar(MultipartFile archivo) { return importarArchivo(archivo); }

    private CatalogImportResponseDTO importarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw invalido("Debe proporcionar un XLSX oficial de SAGE");
        long inicio = System.nanoTime(); int leidos = 0, insertados = 0, actualizados = 0, rechazados = 0;
        try (Workbook libro = WorkbookFactory.create(archivo.getInputStream())) {
            DataFormatter f = new DataFormatter(Locale.ROOT);
            FormulaEvaluator e = libro.getCreationHelper().createFormulaEvaluator();
            Sheet hoja = libro.getSheetAt(0);
            Header h = encontrarHeader(hoja, f, e);
            if (h == null) throw invalido("El Excel no contiene los encabezados oficiales de SAGE Gold ni SAGE Choice");
            for (int fila = h.fila() + 1; fila <= hoja.getLastRowNum(); fila++) {
                Row row = hoja.getRow(fila); if (row == null) continue;
                String titulo = valor(row, h.titulo(), f, e), codigo = valor(row, h.codigo(), f, e);
                if (titulo == null && codigo == null) continue;
                leidos++;
                if (titulo == null || codigo == null) { rechazados++; continue; }
                Optional<SageRevista> existente = repository.findFirstByModeloPublicacionAndJournalCodeIgnoreCase(h.modelo(), codigo);
                SageRevista r = existente.orElseGet(SageRevista::new);
                r.setTitulo(titulo); r.setJournalCode(codigo); r.setModeloPublicacion(h.modelo());
                r.setFuenteArchivo(archivo.getOriginalFilename());
                if (GOLD.equals(h.modelo())) cargarGold(r, row, h, f, e); else cargarChoice(r, row, h, f, e);
                repository.save(r);
                if (existente.isEmpty()) insertados++; else actualizados++;
            }
            repository.flush();
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw invalido("No fue posible leer el Excel de SAGE: " + mensaje(ex)); }
        return new CatalogImportResponseDTO(leidos, insertados, actualizados, rechazados,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    private void cargarGold(SageRevista r, Row row, Header h, DataFormatter f, FormulaEvaluator e) {
        r.setTla(valor(row, h.tla(), f, e)); r.setUrlOficial(valor(row, h.url(), f, e));
        r.setPrecioLista(valor(row, h.precioLista(), f, e)); r.setPrecioActual(valor(row, h.precioActual(), f, e));
        String moneda = valor(row, h.moneda(), f, e); r.setMoneda(moneda);
        r.setApcUsd("USD".equalsIgnoreCase(moneda) ? r.getPrecioActual() : null);
        r.setApcGbp("GBP".equalsIgnoreCase(moneda) ? r.getPrecioActual() : null);
    }

    private void cargarChoice(SageRevista r, Row row, Header h, DataFormatter f, FormulaEvaluator e) {
        String issn = normalizarIssn(valor(row, h.issn(), f, e));
        String eissn = normalizarIssn(valor(row, h.eissn(), f, e));
        r.setIssn(issn == null ? null : formatearIssn(issn)); r.setIssnNormalizado(issn);
        r.setEissn(eissn == null ? null : formatearIssn(eissn)); r.setEissnNormalizado(eissn);
        r.setDivision(valor(row, h.division(), f, e)); r.setApcUsd(valor(row, h.usd(), f, e));
        r.setApcGbp(valor(row, h.gbp(), f, e)); r.setUrlOficial(valor(row, h.url(), f, e));
    }

    @Scheduled(cron = "${sage.pricing.cron:0 30 2 1 * *}")
    public void descargarEImportar() {
        if (!enabled || !running.compareAndSet(false, true)) return;
        try {
            Map<String, URI> enlaces = descubrirEnlaces();
            procesar(enlaces.getOrDefault(GOLD, goldUrlRespaldo), goldHash, "sage-gold-oa.xlsx", GOLD);
            procesar(enlaces.getOrDefault(CHOICE, choiceUrlRespaldo), choiceHash, "sage-choice.xlsx", CHOICE);
        } finally { running.set(false); }
    }

    private Map<String, URI> descubrirEnlaces() {
        Map<String, URI> encontrados = new HashMap<>();
        try {
            HttpRequest request = HttpRequest.newBuilder(paginaOficial).timeout(timeout)
                    .header("Accept", "text/html").header("User-Agent", "SICREC-SAGE-APC/1.0").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return encontrados;
            Matcher matcher = HREF.matcher(response.body());
            while (matcher.find()) {
                String href = matcher.group(1).replace("&amp;", "&");
                String lower = href.toLowerCase(Locale.ROOT);
                if (lower.contains("sage-gold-oa-apcs")) encontrados.put(GOLD, paginaOficial.resolve(href));
                else if (lower.contains("sage-choice-price-list")) encontrados.put(CHOICE, paginaOficial.resolve(href));
            }
        } catch (Exception ex) { LOG.warn("No se pudieron descubrir los enlaces SAGE; se usarán respaldos: {}", mensaje(ex)); }
        return encontrados;
    }

    private void procesar(URI url, Path hashPath, String nombreArchivo, String tipo) {
        try {
            byte[] excel = descargar(url); String hash = CatalogDownloadSupport.calcularSha256(excel);
            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                auditar(true, tipo, "Lista sin cambios; importación omitida. SHA-256: " + hash); return;
            }
            InMemoryMultipartFile archivo = new InMemoryMultipartFile(excel, nombreArchivo, XLSX);
            CatalogImportResponseDTO resultado = transactionTemplate == null ? importarArchivo(archivo)
                    : transactionTemplate.execute(status -> importarArchivo(archivo));
            if (resultado == null) throw new IllegalStateException("La transacción SAGE no devolvió resultado");
            CatalogDownloadSupport.guardarHash(hashPath, hash, "sage-" + tipo.toLowerCase(Locale.ROOT) + "-");
            auditar(true, tipo, "Lista descargada e importada: " + resultado.getRegistrosLeidos() + " registros. SHA-256: " + hash);
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); auditar(false, tipo, "Descarga interrumpida"); }
        catch (Exception ex) { LOG.error("Falló la descarga/importación SAGE {}", tipo, ex); auditar(false, tipo, "Falló la descarga/importación: " + mensaje(ex)); }
    }

    private byte[] descargar(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(url).timeout(timeout).header("Accept", XLSX)
                .header("User-Agent", "SICREC-SAGE-APC/1.0").GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("SAGE respondió con HTTP " + response.statusCode());
        byte[] body = response.body();
        if (body == null || body.length < 4 || body[0] != 'P' || body[1] != 'K') throw new IOException("SAGE no devolvió un XLSX válido");
        return body;
    }

    private Header encontrarHeader(Sheet hoja, DataFormatter f, FormulaEvaluator e) {
        for (int i = hoja.getFirstRowNum(); i <= Math.min(hoja.getLastRowNum(), 20); i++) {
            Row row = hoja.getRow(i); if (row == null) continue; Map<String, Integer> c = new HashMap<>();
            for (Cell cell : row) c.put(normalizarTexto(f.formatCellValue(cell, e)), cell.getColumnIndex());
            Integer titulo = primera(c, "journal title1", "journal title"), codigo = primera(c, "journal code");
            if (titulo == null || codigo == null) continue;
            if (c.containsKey("current price 2026") && c.containsKey("currency"))
                return new Header(i, GOLD, titulo, codigo, indice(c,"tla"), -1,-1,-1,
                        indice(c,"list price 2026"), indice(c,"current price 2026"), indice(c,"currency"),
                        -1,-1, indice(c,"sj site"));
            Integer issn = indice(c,"issn"), eissn = indice(c,"eissn"), usd = contiene(c,"oa apc ($)"), gbp = contiene(c,"oa apc (£)");
            if (issn >= 0 && eissn >= 0 && usd != null && gbp != null)
                return new Header(i, CHOICE, titulo, codigo, -1, issn,eissn,indice(c,"division"),
                        -1,-1,-1,usd,gbp,indice(c,"journal url"));
        }
        return null;
    }

    private Integer contiene(Map<String,Integer> c, String texto) { return c.entrySet().stream().filter(x -> x.getKey().contains(texto)).map(Map.Entry::getValue).findFirst().orElse(null); }
    private Integer primera(Map<String,Integer> c, String... nombres) { for (String n : nombres) if (c.containsKey(n)) return c.get(n); return null; }
    private int indice(Map<String,Integer> c, String nombre) { return c.getOrDefault(nombre, -1); }
    private String valor(Row r, int col, DataFormatter f, FormulaEvaluator e) { if (col < 0) return null; Cell c = r.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL); return c == null ? null : limpiar(f.formatCellValue(c, e)); }
    private JournalInfo convertir(SageRevista r) {
        Map<String,String> d = new LinkedHashMap<>(); poner(d,"modeloPublicacion",r.getModeloPublicacion());
        poner(d,"journalCode",r.getJournalCode()); poner(d,"tla",r.getTla()); poner(d,"division",r.getDivision());
        poner(d,"precioLista",r.getPrecioLista()); poner(d,"precioActual",r.getPrecioActual()); poner(d,"apcUsd",r.getApcUsd()); poner(d,"apcGbp",r.getApcGbp());
        poner(d,"openAccess","Sí");
        String apc = primerNoVacio(r.getApcUsd(), r.getApcGbp(), r.getPrecioActual());
        String moneda = r.getApcUsd() != null ? "USD" : r.getApcGbp() != null ? "GBP" : r.getMoneda();
        return new JournalInfo(nombre(),r.getTitulo(),r.getIssn(),r.getEissn(),"SAGE",null,null,null,null,apc,moneda,r.getUrlOficial(),d);
    }
    private String primerNoVacio(String... valores) { for (String v : valores) if (v != null) return v; return null; }
    private String normalizarIssn(String v) { if (v == null) return null; String n=v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]",""); return n.matches("\\d{7}[\\dX]")?n:null; }
    private String formatearIssn(String n) { return n.substring(0,4)+"-"+n.substring(4); }
    private String normalizarTexto(String v) { return v == null ? "" : v.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
    private String limpiar(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String mensaje(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private ResponseStatusException invalido(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
    private void poner(Map<String,String> d,String k,String v) { if(v!=null)d.put(k,v); }
    private void auditar(boolean exito,String tipo,String descripcion) { try { String accion="IMPORTACIÓN_APC_AUTOMÁTICA_"+tipo; if(exito)auditoria.registrarExito(null,"SAGE",accion,descripcion); else auditoria.registrarError(null,"SAGE",accion,descripcion); } catch(RuntimeException ex){LOG.error("No se pudo registrar la auditoría SAGE",ex);} }
    private record Header(int fila,String modelo,int titulo,int codigo,int tla,int issn,int eissn,int division,
                          int precioLista,int precioActual,int moneda,int usd,int gbp,int url) { }
}
