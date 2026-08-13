package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.*;
import uteq.edu.ec.crecuteq.entity.BrillRevista;
import uteq.edu.ec.crecuteq.etl.*;
import uteq.edu.ec.crecuteq.repository.BrillRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Proveedor individual Brill basado en la lista XLSX Brill-EIFL 2026. */
@Component
public class BrillProvider implements JournalProvider {
    private static final Logger LOG=LoggerFactory.getLogger(BrillProvider.class);
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final BrillRevistaRepository repository; private final AuditoriaService auditoria;
    private final HttpClient http; private final URI url; private final String sourceUrl; private final Duration timeout;
    private final Path hashPath; private final boolean enabled; private final AtomicBoolean running=new AtomicBoolean(false);
    private TransactionTemplate transactions;
    @Autowired public BrillProvider(BrillRevistaRepository repository,AuditoriaService auditoria,
            @Value("${brill.pricing.url}") String url,@Value("${brill.pricing.source-url}") String sourceUrl,
            @Value("${brill.pricing.connect-timeout-ms:10000}") long connect,@Value("${brill.pricing.read-timeout-ms:120000}") long read,
            @Value("${brill.pricing.hash-file:data/brill-apc.sha256}") String hash,@Value("${brill.pricing.enabled:true}") boolean enabled){
        this.repository=repository;this.auditoria=auditoria;this.http=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connect)).followRedirects(HttpClient.Redirect.NORMAL).build();this.url=URI.create(url);this.sourceUrl=sourceUrl;this.timeout=Duration.ofMillis(read);this.hashPath=Path.of(hash);this.enabled=enabled;}
    @Autowired void configurarTransacciones(PlatformTransactionManager m){transactions=new TransactionTemplate(m);}
    @Override public String nombre(){return "brill";}
    @Override @Transactional(readOnly=true) public Optional<JournalInfo> buscar(String productId,String issn,String eissn){String i=normalizarIssn(issn),e=normalizarIssn(eissn);if(i==null&&e==null)return Optional.empty();return repository.buscarPorIdentificadores(i,e).map(this::convertir);}
    @Transactional public CatalogImportResponseDTO importar(MultipartFile archivo){return importarArchivo(archivo);}
    private CatalogImportResponseDTO importarArchivo(MultipartFile archivo){if(archivo==null||archivo.isEmpty())throw invalido("Debe proporcionar el XLSX Brill-EIFL");long inicio=System.nanoTime();int leidos=0,insertados=0,actualizados=0,rechazados=0;
        try(Workbook w=WorkbookFactory.create(archivo.getInputStream())){DataFormatter f=new DataFormatter(Locale.ROOT);FormulaEvaluator ev=w.getCreationHelper().createFormulaEvaluator();Sheet s=w.getSheetAt(0);Header h=header(s,f,ev);if(h==null)throw invalido("El Excel no contiene los encabezados esperados de Brill-EIFL");
            for(int n=h.fila()+1;n<=s.getLastRowNum();n++){Row row=s.getRow(n);if(row==null)continue;String titulo=valor(row,h.titulo(),f,ev),eissn=normalizarIssn(valor(row,h.eissn(),f,ev)),editorial=valor(row,h.editorial(),f,ev);if(titulo==null&&eissn==null)continue;if(!"brill".equalsIgnoreCase(editorial))continue;leidos++;if(titulo==null||eissn==null){rechazados++;continue;}Optional<BrillRevista> existente=repository.findFirstByEissnNormalizado(eissn);BrillRevista r=existente.orElseGet(BrillRevista::new);r.setEissnNormalizado(eissn);r.setEissn(eissn.substring(0,4)+"-"+eissn.substring(4));r.setTitulo(titulo);r.setEditorial(editorial);r.setApcEur(normalizarApc(valor(row,h.apc(),f,ev)));r.setTipoOa(valor(row,h.tipoOa(),f,ev));r.setAreaTematica(valor(row,h.area(),f,ev));r.setSubdisciplina(valor(row,h.subdisciplina(),f,ev));r.setFactorImpacto(valor(row,h.factor(),f,ev));r.setIdioma(valor(row,h.idioma(),f,ev));r.setUrlOficial(normalizarUrl(valor(row,h.url(),f,ev)));r.setFuenteArchivo(archivo.getOriginalFilename());repository.save(r);if(existente.isEmpty())insertados++;else actualizados++;}repository.flush();
        }catch(ResponseStatusException x){throw x;}catch(Exception x){throw invalido("No fue posible leer el Excel Brill-EIFL: "+mensaje(x));}return new CatalogImportResponseDTO(leidos,insertados,actualizados,rechazados,(System.nanoTime()-inicio)/1_000_000);}
    @Scheduled(cron="${brill.pricing.cron:0 30 3 1 * *}") public void descargarEImportar(){if(!enabled||!running.compareAndSet(false,true))return;try{byte[]b=descargar();String hash=CatalogDownloadSupport.calcularSha256(b);if(hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))){auditar(true,"Lista sin cambios; importación omitida. SHA-256: "+hash);return;}InMemoryMultipartFile a=new InMemoryMultipartFile(b,"brill-apc-2026.xlsx",XLSX);CatalogImportResponseDTO r=transactions==null?importarArchivo(a):transactions.execute(x->importarArchivo(a));if(r==null)throw new IllegalStateException("La transacción Brill no devolvió resultado");CatalogDownloadSupport.guardarHash(hashPath,hash,"brill-apc-");auditar(true,"Lista importada: "+r.getRegistrosLeidos()+" registros. SHA-256: "+hash);}catch(InterruptedException x){Thread.currentThread().interrupt();auditar(false,"Descarga interrumpida");}catch(Exception x){LOG.error("Falló la descarga/importación Brill",x);auditar(false,"Falló la descarga/importación: "+mensaje(x));}finally{running.set(false);}}
    private byte[] descargar()throws IOException,InterruptedException{HttpRequest q=HttpRequest.newBuilder(url).timeout(timeout).header("Accept",XLSX).header("User-Agent","SICREC-Brill-APC/1.0").GET().build();HttpResponse<byte[]>r=http.send(q,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()<200||r.statusCode()>=300)throw new IOException("Google Sheets respondió con HTTP "+r.statusCode());byte[]b=r.body();if(b==null||b.length<4||b[0]!='P'||b[1]!='K')throw new IOException("La fuente Brill-EIFL no devolvió un XLSX válido");return b;}
    private Header header(Sheet s,DataFormatter f,FormulaEvaluator e){for(int n=s.getFirstRowNum();n<=Math.min(20,s.getLastRowNum());n++){Row r=s.getRow(n);if(r==null)continue;Map<String,Integer>m=new HashMap<>();for(Cell c:r)m.put(normalizarTexto(f.formatCellValue(c,e)),c.getColumnIndex());if(m.containsKey("journal")&&m.containsKey("e issn")&&m.containsKey("apc list price"))return new Header(n,m.get("subject area"),m.get("sub discipline"),m.get("journal"),m.get("e issn"),m.get("publisher"),m.get("apc list price"),m.get("impact factor"),m.get("oa type"),m.get("url"),m.get("language s"));}return null;}
    private JournalInfo convertir(BrillRevista r){Map<String,String>d=new LinkedHashMap<>();poner(d,"tipoOa",r.getTipoOa());poner(d,"apcEur",r.getApcEur());poner(d,"areaTematica",r.getAreaTematica());poner(d,"subdisciplina",r.getSubdisciplina());poner(d,"factorImpacto",r.getFactorImpacto());poner(d,"openAccess",r.getTipoOa());return new JournalInfo(nombre(),r.getTitulo(),null,r.getEissn(),r.getEditorial(),null,r.getIdioma(),r.getAreaTematica(),null,r.getApcEur(),r.getApcEur()==null?null:"EUR",r.getUrlOficial()==null?sourceUrl:r.getUrlOficial(),d);}
    private String valor(Row r,int n,DataFormatter f,FormulaEvaluator e){Cell c=r.getCell(n,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);return c==null?null:limpiar(f.formatCellValue(c,e));}private String normalizarIssn(String v){if(v==null)return null;String n=v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]","");return n.matches("\\d{7}[\\dX]")?n:null;}private String normalizarTexto(String v){return v==null?"":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}private String normalizarApc(String v){if(v==null)return null;return v.replace("€","").replace(".","").replace(",","").replaceAll("\\s+","");}private String normalizarUrl(String v){if(v==null)return null;return v.matches("(?i)^https?://.*")?v:"https://"+v;}private String limpiar(String v){return v==null||v.isBlank()?null:v.trim();}private String mensaje(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}private ResponseStatusException invalido(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private void poner(Map<String,String>d,String k,String v){if(v!=null)d.put(k,v);}private void auditar(boolean ok,String d){try{if(ok)auditoria.registrarExito(null,"BRILL","IMPORTACIÓN_APC_AUTOMÁTICA",d);else auditoria.registrarError(null,"BRILL","IMPORTACIÓN_APC_AUTOMÁTICA",d);}catch(RuntimeException x){LOG.error("No se pudo registrar auditoría Brill",x);}}
    private record Header(int fila,int area,int subdisciplina,int titulo,int eissn,int editorial,int apc,int factor,int tipoOa,int url,int idioma){}
}
