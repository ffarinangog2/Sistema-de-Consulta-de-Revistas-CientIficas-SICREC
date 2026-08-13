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
import uteq.edu.ec.crecuteq.entity.DegruyterRevista;
import uteq.edu.ec.crecuteq.etl.*;
import uteq.edu.ec.crecuteq.repository.DegruyterRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Proveedor individual de Walter de Gruyter basado en su XLSX oficial. */
@Component
public class DegruyterProvider implements JournalProvider {
    private static final Logger LOG=LoggerFactory.getLogger(DegruyterProvider.class);
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final DegruyterRevistaRepository repository; private final AuditoriaService auditoria;
    private final HttpClient http; private final URI url; private final String sourceUrl; private final Duration timeout;
    private final Path hashPath; private final boolean enabled; private final AtomicBoolean running=new AtomicBoolean(false);
    private TransactionTemplate transactions;
    @Autowired public DegruyterProvider(DegruyterRevistaRepository repository,AuditoriaService auditoria,
            @Value("${degruyter.pricing.url}") String url,@Value("${degruyter.pricing.source-url}") String sourceUrl,
            @Value("${degruyter.pricing.connect-timeout-ms:10000}") long connect,@Value("${degruyter.pricing.read-timeout-ms:120000}") long read,
            @Value("${degruyter.pricing.hash-file:data/degruyter-apc.sha256}") String hash,@Value("${degruyter.pricing.enabled:true}") boolean enabled){
        this(repository,auditoria,HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connect)).followRedirects(HttpClient.Redirect.NORMAL).build(),URI.create(url),sourceUrl,Duration.ofMillis(read),Path.of(hash),enabled);}
    DegruyterProvider(DegruyterRevistaRepository r,AuditoriaService a,HttpClient h,URI u,String s,Duration t,Path p,boolean e){repository=r;auditoria=a;http=h;url=u;sourceUrl=s;timeout=t;hashPath=p;enabled=e;}
    @Autowired void configurarTransacciones(PlatformTransactionManager m){transactions=new TransactionTemplate(m);}
    @Override public String nombre(){return "degruyter";}
    @Override @Transactional(readOnly=true) public Optional<JournalInfo> buscar(String productId,String issn,String eissn){String i=normalizarIssn(issn),e=normalizarIssn(eissn);Optional<DegruyterRevista>r=(i==null&&e==null)?Optional.empty():repository.findFirstByIssnNormalizadoOrEissnNormalizado(i,e);if(r.isEmpty()&&limpiar(productId)!=null)r=repository.findFirstByCodigoOnlineIgnoreCase(productId.trim());return r.map(this::convertir);}
    @Transactional public CatalogImportResponseDTO importar(MultipartFile archivo){return importarArchivo(archivo);}
    private CatalogImportResponseDTO importarArchivo(MultipartFile archivo){if(archivo==null||archivo.isEmpty())throw invalido("Debe proporcionar el XLSX oficial de De Gruyter");long inicio=System.nanoTime();int leidos=0,insertados=0,actualizados=0,rechazados=0;
        try(Workbook w=WorkbookFactory.create(archivo.getInputStream())){DataFormatter f=new DataFormatter(Locale.ROOT);FormulaEvaluator ev=w.getCreationHelper().createFormulaEvaluator();Sheet s=w.getSheetAt(0);Header h=header(s,f,ev);if(h==null)throw invalido("El Excel no contiene los encabezados oficiales de De Gruyter");
            for(int n=h.fila()+1;n<=s.getLastRowNum();n++){Row row=s.getRow(n);if(row==null)continue;String codigo=valor(row,h.codigo(),f,ev),titulo=valor(row,h.titulo(),f,ev),tipo=valor(row,h.tipo(),f,ev),publisher=valor(row,h.publisher(),f,ev);if(codigo==null&&titulo==null)continue;if(!"journal".equalsIgnoreCase(tipo)||!esDeGruyter(publisher))continue;leidos++;if(codigo==null||titulo==null){rechazados++;continue;}
                Optional<DegruyterRevista> existente=repository.findFirstByCodigoOnlineIgnoreCase(codigo);DegruyterRevista r=existente.orElseGet(DegruyterRevista::new);r.setCodigoOnline(codigo);r.setTitulo(titulo);asignarIssn(r,valor(row,h.issn(),f,ev),false);asignarIssn(r,valor(row,h.eissn(),f,ev),true);r.setEditorial(publisher);String modelo=valor(row,h.modelo(),f,ev);r.setModeloPublicacion(modelo);String apc=valor(row,h.apc(),f,ev);if(apc==null&&esSinCargo(modelo))apc="0";r.setApcEur(apc);r.setAreaTematica(valor(row,h.area(),f,ev));r.setIdioma(valor(row,h.idioma(),f,ev));r.setLicencia(valor(row,h.licencia(),f,ev));r.setUrlOficial(valor(row,h.url(),f,ev));r.setFuenteArchivo(archivo.getOriginalFilename());repository.save(r);if(existente.isEmpty())insertados++;else actualizados++;}repository.flush();
        }catch(ResponseStatusException x){throw x;}catch(Exception x){throw invalido("No fue posible leer el Excel de De Gruyter: "+mensaje(x));}return new CatalogImportResponseDTO(leidos,insertados,actualizados,rechazados,(System.nanoTime()-inicio)/1_000_000);}
    @Scheduled(cron="${degruyter.pricing.cron:0 15 3 1 * *}") public void descargarEImportar(){if(!enabled||!running.compareAndSet(false,true))return;try{byte[]b=descargar();String hash=CatalogDownloadSupport.calcularSha256(b);if(hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))){auditar(true,"Lista sin cambios; importación omitida. SHA-256: "+hash);return;}InMemoryMultipartFile a=new InMemoryMultipartFile(b,"degruyter-apc-2026.xlsx",XLSX);CatalogImportResponseDTO r=transactions==null?importarArchivo(a):transactions.execute(x->importarArchivo(a));if(r==null)throw new IllegalStateException("La transacción De Gruyter no devolvió resultado");CatalogDownloadSupport.guardarHash(hashPath,hash,"degruyter-apc-");auditar(true,"Lista importada: "+r.getRegistrosLeidos()+" registros. SHA-256: "+hash);}catch(InterruptedException x){Thread.currentThread().interrupt();auditar(false,"Descarga interrumpida");}catch(Exception x){LOG.error("Falló la descarga/importación De Gruyter",x);auditar(false,"Falló la descarga/importación: "+mensaje(x));}finally{running.set(false);}}
    private byte[] descargar()throws IOException,InterruptedException{HttpRequest q=HttpRequest.newBuilder(url).timeout(timeout).header("Accept",XLSX).header("User-Agent","SICREC-DeGruyter-APC/1.0").GET().build();HttpResponse<byte[]>r=http.send(q,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()<200||r.statusCode()>=300)throw new IOException("De Gruyter respondió con HTTP "+r.statusCode());byte[]b=r.body();if(b==null||b.length<4||b[0]!='P'||b[1]!='K')throw new IOException("De Gruyter no devolvió un XLSX válido");return b;}
    private Header header(Sheet s,DataFormatter f,FormulaEvaluator e){for(int n=s.getFirstRowNum();n<=Math.min(20,s.getLastRowNum());n++){Row r=s.getRow(n);if(r==null)continue;Map<String,Integer>m=new HashMap<>();for(Cell c:r)m.put(normalizarTexto(f.formatCellValue(c,e)),c.getColumnIndex());if(m.containsKey("journal code online")&&m.containsKey("online issn")&&m.containsKey("apc eur"))return new Header(n,m.get("journal code online"),m.get("title"),m.get("product type"),m.get("print issn"),m.get("online issn"),m.get("publisher"),m.get("publishing model"),m.get("apc eur"),m.get("subject area"),m.get("main language"),m.get("creative commons license"),m.get("url"));}return null;}
    private boolean esDeGruyter(String p){if(p==null)return false;String n=normalizarTexto(p);return n.startsWith("de gruyter")||n.startsWith("walter de gruyter")||n.equals("oldenbourg wissenschaftsverlag");}
    private boolean esSinCargo(String m){String n=normalizarTexto(m);return n.contains("sponsored")||n.equals("s2o")||n.contains("subscribe to open");}
    private void asignarIssn(DegruyterRevista r,String v,boolean online){String n=normalizarIssn(v),f=n==null?null:n.substring(0,4)+"-"+n.substring(4);if(online){r.setEissn(f);r.setEissnNormalizado(n);}else{r.setIssn(f);r.setIssnNormalizado(n);}}
    private JournalInfo convertir(DegruyterRevista r){Map<String,String>d=new LinkedHashMap<>();poner(d,"modeloPublicacion",r.getModeloPublicacion());poner(d,"apcEur",r.getApcEur());poner(d,"areaTematica",r.getAreaTematica());poner(d,"licencias",r.getLicencia());poner(d,"openAccess",r.getModeloPublicacion());return new JournalInfo(nombre(),r.getTitulo(),r.getIssn(),r.getEissn(),r.getEditorial(),null,r.getIdioma(),r.getAreaTematica(),r.getLicencia(),r.getApcEur(),r.getApcEur()==null?null:"EUR",r.getUrlOficial()==null?sourceUrl:r.getUrlOficial(),d);}
    private String valor(Row r,int n,DataFormatter f,FormulaEvaluator e){Cell c=r.getCell(n,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);return c==null?null:limpiar(f.formatCellValue(c,e));}private String normalizarIssn(String v){if(v==null)return null;String n=v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]","");return n.matches("\\d{7}[\\dX]")?n:null;}private String normalizarTexto(String v){return v==null?"":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}private String limpiar(String v){return v==null||v.isBlank()?null:v.trim();}private String mensaje(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}private ResponseStatusException invalido(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private void poner(Map<String,String>d,String k,String v){if(v!=null)d.put(k,v);}private void auditar(boolean ok,String d){try{if(ok)auditoria.registrarExito(null,"DEGRUYTER","IMPORTACIÓN_APC_AUTOMÁTICA",d);else auditoria.registrarError(null,"DEGRUYTER","IMPORTACIÓN_APC_AUTOMÁTICA",d);}catch(RuntimeException x){LOG.error("No se pudo registrar auditoría De Gruyter",x);}}
    private record Header(int fila,int codigo,int titulo,int tipo,int issn,int eissn,int publisher,int modelo,int apc,int area,int idioma,int licencia,int url){}
}
