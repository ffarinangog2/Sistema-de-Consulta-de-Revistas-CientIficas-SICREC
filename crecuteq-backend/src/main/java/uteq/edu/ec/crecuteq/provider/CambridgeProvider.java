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
import uteq.edu.ec.crecuteq.entity.CambridgeRevista;
import uteq.edu.ec.crecuteq.etl.*;
import uteq.edu.ec.crecuteq.repository.CambridgeRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Proveedor basado exclusivamente en el XLSX oficial de APC de Cambridge. */
@Component
public class CambridgeProvider implements JournalProvider {
    private static final Logger LOG=LoggerFactory.getLogger(CambridgeProvider.class);
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final CambridgeRevistaRepository repository; private final AuditoriaService auditoria;
    private final HttpClient http; private final URI url; private final Duration timeout; private final Path hashPath;
    private final boolean enabled; private final AtomicBoolean running=new AtomicBoolean(false); private TransactionTemplate transactions;
    @Autowired public CambridgeProvider(CambridgeRevistaRepository repository,AuditoriaService auditoria,
        @Value("${cambridge.pricing.url}") String url,@Value("${cambridge.pricing.connect-timeout-ms:10000}") long connect,
        @Value("${cambridge.pricing.read-timeout-ms:120000}") long read,@Value("${cambridge.pricing.hash-file:data/cambridge-apc.sha256}") String hash,
        @Value("${cambridge.pricing.enabled:true}") boolean enabled){this(repository,auditoria,HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connect)).followRedirects(HttpClient.Redirect.NORMAL).build(),URI.create(url),Duration.ofMillis(read),Path.of(hash),enabled);}
    CambridgeProvider(CambridgeRevistaRepository r,AuditoriaService a,HttpClient h,URI u,Duration t,Path p,boolean e){repository=r;auditoria=a;http=h;url=u;timeout=t;hashPath=p;enabled=e;}
    @Autowired void configurarTransacciones(PlatformTransactionManager m){transactions=new TransactionTemplate(m);}
    @Override public String nombre(){return "cambridge";}
    @Override @Transactional(readOnly=true) public Optional<JournalInfo> buscar(String productId,String issn,String eissn){
        String i=normalizarIssn(issn),e=normalizarIssn(eissn); Optional<CambridgeRevista> r=(i==null&&e==null)?Optional.empty():repository.findFirstByIssnNormalizadoOrEissnNormalizado(i,e);
        if(r.isEmpty()&&limpiar(productId)!=null)r=repository.findFirstByMnemonicIgnoreCase(productId.trim()); return r.map(this::convertir);
    }
    @Transactional public CatalogImportResponseDTO importar(MultipartFile archivo){return importarArchivo(archivo);}
    private CatalogImportResponseDTO importarArchivo(MultipartFile archivo){
        if(archivo==null||archivo.isEmpty())throw invalido("Debe proporcionar el XLSX oficial de APC de Cambridge"); long inicio=System.nanoTime();int leidos=0,insertados=0,actualizados=0,rechazados=0;
        try(Workbook libro=WorkbookFactory.create(archivo.getInputStream())){DataFormatter f=new DataFormatter(Locale.ROOT);FormulaEvaluator e=libro.getCreationHelper().createFormulaEvaluator();Sheet hoja=libro.getSheetAt(0);Header h=encontrarHeader(hoja,f,e);if(h==null)throw invalido("El Excel no contiene los encabezados oficiales de Cambridge");
            for(int n=h.filaDatos();n<=hoja.getLastRowNum();n++){Row row=hoja.getRow(n);if(row==null)continue;String mnemonic=valor(row,h.mnemonic(),f,e),titulo=valor(row,h.titulo(),f,e);if(mnemonic==null&&titulo==null)continue;leidos++;if(mnemonic==null||titulo==null){rechazados++;continue;}
                Optional<CambridgeRevista> existente=repository.findFirstByMnemonicIgnoreCase(mnemonic);CambridgeRevista r=existente.orElseGet(CambridgeRevista::new);r.setMnemonic(mnemonic);r.setTitulo(titulo);String issn=normalizarIssn(valor(row,h.issn(),f,e)),eissn=normalizarIssn(valor(row,h.eissn(),f,e));r.setIssn(issn==null?null:formatearIssn(issn));r.setIssnNormalizado(issn);r.setEissn(eissn==null?null:formatearIssn(eissn));r.setEissnNormalizado(eissn);r.setArea(valor(row,h.area(),f,e));r.setModeloPublicacion(valor(row,h.modelo(),f,e));r.setApcGbp(valor(row,h.gbp(),f,e));r.setApcGbpMiembro(valor(row,h.gbpMiembro(),f,e));r.setApcUsd(valor(row,h.usd(),f,e));r.setApcUsdMiembro(valor(row,h.usdMiembro(),f,e));r.setApcEur(valor(row,h.eur(),f,e));r.setApcEurMiembro(valor(row,h.eurMiembro(),f,e));r.setApcAud(valor(row,h.aud(),f,e));r.setApcAudMiembro(valor(row,h.audMiembro(),f,e));r.setNotasApc(valor(row,h.notas(),f,e));r.setLicencias(valor(row,h.licencias(),f,e));r.setUrlOficial(valor(row,h.url(),f,e));r.setFuenteArchivo(archivo.getOriginalFilename());repository.save(r);if(existente.isEmpty())insertados++;else actualizados++;}repository.flush();
        }catch(ResponseStatusException x){throw x;}catch(Exception x){throw invalido("No fue posible leer el Excel de Cambridge: "+mensaje(x));}return new CatalogImportResponseDTO(leidos,insertados,actualizados,rechazados,(System.nanoTime()-inicio)/1_000_000);
    }
    @Scheduled(cron="${cambridge.pricing.cron:0 45 2 1 * *}") public void descargarEImportar(){if(!enabled||!running.compareAndSet(false,true))return;try{byte[] excel=descargar();String hash=CatalogDownloadSupport.calcularSha256(excel);if(hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))){auditar(true,"Lista sin cambios; importación omitida. SHA-256: "+hash);return;}InMemoryMultipartFile a=new InMemoryMultipartFile(excel,"cambridge-apc.xlsx",XLSX);CatalogImportResponseDTO r=transactions==null?importarArchivo(a):transactions.execute(s->importarArchivo(a));if(r==null)throw new IllegalStateException("La transacción Cambridge no devolvió resultado");CatalogDownloadSupport.guardarHash(hashPath,hash,"cambridge-apc-");auditar(true,"Lista descargada e importada: "+r.getRegistrosLeidos()+" registros. SHA-256: "+hash);}catch(InterruptedException x){Thread.currentThread().interrupt();auditar(false,"Descarga interrumpida");}catch(Exception x){LOG.error("Falló la descarga/importación Cambridge",x);auditar(false,"Falló la descarga/importación: "+mensaje(x));}finally{running.set(false);}}
    private byte[] descargar()throws IOException,InterruptedException{HttpRequest q=HttpRequest.newBuilder(url).timeout(timeout).header("Accept",XLSX).header("User-Agent","SICREC-Cambridge-APC/1.0").GET().build();HttpResponse<byte[]>r=http.send(q,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()<200||r.statusCode()>=300)throw new IOException("Cambridge respondió con HTTP "+r.statusCode());byte[]b=r.body();if(b==null||b.length<4||b[0]!='P'||b[1]!='K')throw new IOException("Cambridge no devolvió un XLSX válido");return b;}
    private Header encontrarHeader(Sheet s,DataFormatter f,FormulaEvaluator e){for(int n=s.getFirstRowNum();n<=Math.min(s.getLastRowNum(),20);n++){Row r=s.getRow(n);if(r==null)continue;Map<String,Integer>c=columnas(r,f,e);if(!c.containsKey("mnemonic")||!c.containsKey("journal")||!c.containsKey("print issn"))continue;for(int k=n;k<=Math.min(n+4,s.getLastRowNum());k++){Map<String,Integer>p=columnas(s.getRow(k),f,e);if(p.containsKey("us $")&&p.containsKey("gb £"))return new Header(k+1,c.get("mnemonic"),c.get("journal"),c.get("print issn"),c.get("online issn"),c.get("hss/stm"),c.get("open access status"),p.get("gb £"),p.getOrDefault("gb £ member rate",-1),p.get("us $"),p.getOrDefault("us $ member rate",-1),p.getOrDefault("eur €",-1),p.getOrDefault("eur € member rate",-1),p.getOrDefault("aud",-1),p.getOrDefault("aud member rate",-1),p.getOrDefault("apc notes",-1),c.getOrDefault("gold oa cc licence options",-1),c.getOrDefault("url",-1));}}return null;}
    private Map<String,Integer> columnas(Row r,DataFormatter f,FormulaEvaluator e){Map<String,Integer>m=new HashMap<>();if(r!=null)for(Cell c:r)m.put(normalizarTexto(f.formatCellValue(c,e)),c.getColumnIndex());return m;}
    private JournalInfo convertir(CambridgeRevista r){Map<String,String>d=new LinkedHashMap<>();poner(d,"modeloPublicacion",r.getModeloPublicacion());poner(d,"areaTematica",r.getArea());poner(d,"apcUsd",r.getApcUsd());poner(d,"apcGbp",r.getApcGbp());poner(d,"apcEur",r.getApcEur());poner(d,"apcAud",r.getApcAud());poner(d,"apcUsdMiembro",r.getApcUsdMiembro());poner(d,"apcGbpMiembro",r.getApcGbpMiembro());poner(d,"apcEurMiembro",r.getApcEurMiembro());poner(d,"apcAudMiembro",r.getApcAudMiembro());poner(d,"licencias",r.getLicencias());poner(d,"notasApc",r.getNotasApc());poner(d,"openAccess",r.getModeloPublicacion()!=null&&r.getModeloPublicacion().toLowerCase(Locale.ROOT).contains("gold")?"Sí":"Opcional");String apc=primero(r.getApcUsd(),r.getApcGbp(),r.getApcEur(),r.getApcAud());String moneda=r.getApcUsd()!=null?"USD":r.getApcGbp()!=null?"GBP":r.getApcEur()!=null?"EUR":r.getApcAud()!=null?"AUD":null;return new JournalInfo(nombre(),r.getTitulo(),r.getIssn(),r.getEissn(),"Cambridge University Press",null,null,r.getArea(),r.getLicencias(),apc,moneda,r.getUrlOficial(),d);}
    private String valor(Row r,int n,DataFormatter f,FormulaEvaluator e){if(n<0)return null;Cell c=r.getCell(n,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);return c==null?null:limpiar(f.formatCellValue(c,e));}private String primero(String...v){for(String x:v)if(x!=null)return x;return null;}private String normalizarIssn(String v){if(v==null)return null;String n=v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]","");return n.matches("\\d{7}[\\dX]")?n:null;}private String formatearIssn(String n){return n.substring(0,4)+"-"+n.substring(4);}private String normalizarTexto(String v){return v==null?"":v.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}private String limpiar(String v){return v==null||v.isBlank()?null:v.trim();}private String mensaje(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}private ResponseStatusException invalido(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private void poner(Map<String,String>d,String k,String v){if(v!=null)d.put(k,v);}private void auditar(boolean ok,String d){try{if(ok)auditoria.registrarExito(null,"CAMBRIDGE","IMPORTACIÓN_APC_AUTOMÁTICA",d);else auditoria.registrarError(null,"CAMBRIDGE","IMPORTACIÓN_APC_AUTOMÁTICA",d);}catch(RuntimeException x){LOG.error("No se pudo registrar auditoría Cambridge",x);}}
    private record Header(int filaDatos,int mnemonic,int titulo,int issn,int eissn,int area,int modelo,int gbp,int gbpMiembro,int usd,int usdMiembro,int eur,int eurMiembro,int aud,int audMiembro,int notas,int licencias,int url){}
}
