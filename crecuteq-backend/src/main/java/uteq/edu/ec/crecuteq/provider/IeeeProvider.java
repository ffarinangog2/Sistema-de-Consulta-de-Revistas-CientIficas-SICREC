package uteq.edu.ec.crecuteq.provider;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.IeeeRevista;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.repository.IeeeRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Proveedor IEEE basado exclusivamente en los PDF oficiales de tarifas y títulos. */
@Component
public class IeeeProvider implements JournalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(IeeeProvider.class);
    private static final String PDF = "application/pdf";
    private static final Pattern TITLE_ROW = Pattern.compile(
            "(?s)([A-Za-z0-9&./-]{2,25})\\s+(IEEE .+?)\\s+(Full|Hybrid) Open Access\\s+(N/A|\\d{4}-[0-9X]{4})\\s+(N/A|\\d{4}-[0-9X]{4})\\s+");
    private static final Pattern APC_ROW = Pattern.compile(
            "^(.*?)([A-Z][A-Z0-9/-]{1,20})(Full|Hybrid|No OA)(\\$[0-9,]+(?:\\.[0-9]{2})?)?(.*)$");
    private static final Pattern MONEY = Pattern.compile("\\$[0-9,]+(?:\\.[0-9]{2})?(?:/\\$?[0-9,]+)?");
    private static final Set<String> NOISE = Set.of("ieee", "trans", "transactions", "journal", "open", "letters", "mag", "magazine", "the", "of", "on", "and", "in");

    private final IeeeRevistaRepository repository;
    private final AuditoriaService auditoria;
    private final HttpClient http;
    private final URI apcUrl;
    private final URI titlesUrl;
    private final String sourceUrl;
    private final Duration timeout;
    private final Path hashPath;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private TransactionTemplate transactions;

    @Autowired
    public IeeeProvider(IeeeRevistaRepository repository, AuditoriaService auditoria,
            @Value("${ieee.pricing.apc-url}") String apcUrl,
            @Value("${ieee.pricing.titles-url}") String titlesUrl,
            @Value("${ieee.pricing.source-url:https://open.ieee.org/for-authors/article-processing-charges/}") String sourceUrl,
            @Value("${ieee.pricing.connect-timeout-ms:10000}") long connectMs,
            @Value("${ieee.pricing.read-timeout-ms:120000}") long readMs,
            @Value("${ieee.pricing.hash-file:data/ieee-apc.sha256}") String hashFile,
            @Value("${ieee.pricing.enabled:true}") boolean enabled) {
        this(repository, auditoria, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs))
                .followRedirects(HttpClient.Redirect.NORMAL).build(), URI.create(apcUrl), URI.create(titlesUrl),
                sourceUrl, Duration.ofMillis(readMs), Path.of(hashFile), enabled);
    }

    IeeeProvider(IeeeRevistaRepository repository, AuditoriaService auditoria, HttpClient http,
            URI apcUrl, URI titlesUrl, String sourceUrl, Duration timeout, Path hashPath, boolean enabled) {
        this.repository=repository; this.auditoria=auditoria; this.http=http; this.apcUrl=apcUrl;
        this.titlesUrl=titlesUrl; this.sourceUrl=sourceUrl; this.timeout=timeout; this.hashPath=hashPath; this.enabled=enabled;
    }

    @Autowired void configurarTransacciones(PlatformTransactionManager manager){transactions=new TransactionTemplate(manager);}
    @Override public String nombre(){return "ieee";}

    @Override @Transactional(readOnly=true)
    public Optional<JournalInfo> buscar(String productId, String issn, String eissn) {
        String i=normalizarIssn(issn), e=normalizarIssn(eissn);
        Optional<IeeeRevista> resultado=(i==null&&e==null)?Optional.empty():repository.findFirstByIssnNormalizadoOrEissnNormalizado(i,e);
        if(resultado.isEmpty()&&limpiar(productId)!=null)resultado=repository.findFirstByAcronimoIgnoreCase(productId.trim());
        return resultado.map(this::convertir);
    }

    @Scheduled(cron="${ieee.pricing.cron:0 0 3 1 * *}")
    public void descargarEImportar(){
        if(!enabled||!running.compareAndSet(false,true))return;
        try{
            byte[] apc=descargar(apcUrl), titulos=descargar(titlesUrl);
            String hash=CatalogDownloadSupport.calcularSha256(concatenar(apc,titulos));
            if(hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))){auditar(true,"Listas sin cambios; importación omitida. SHA-256: "+hash);return;}
            CatalogImportResponseDTO respuesta=transactions==null?importarArchivos(apc,titulos):transactions.execute(s->importarArchivos(apc,titulos));
            if(respuesta==null)throw new IllegalStateException("La transacción IEEE no devolvió resultado");
            CatalogDownloadSupport.guardarHash(hashPath,hash,"ieee-apc-");
            auditar(true,"Listas descargadas e importadas: "+respuesta.getRegistrosLeidos()+" registros; "+respuesta.getRegistrosRechazados()+" sin correspondencia. SHA-256: "+hash);
        }catch(InterruptedException ex){Thread.currentThread().interrupt();auditar(false,"Descarga interrumpida");}
        catch(Exception ex){LOG.error("Falló la descarga/importación IEEE",ex);auditar(false,"Falló la descarga/importación: "+mensaje(ex));}
        finally{running.set(false);}
    }

    CatalogImportResponseDTO importarArchivos(byte[] apcPdf, byte[] titlesPdf){
        long inicio=System.nanoTime(); int insertados=0,actualizados=0,rechazados=0;
        List<TitleEntry> titulos=parsearTitulos(titlesPdf); List<ApcEntry> tarifas=parsearTarifas(apcPdf);
        if(titulos.size()<100||tarifas.size()<100)throw new IllegalArgumentException("Los PDF de IEEE no contienen la estructura oficial esperada");
        Map<String,TitleEntry> porAcronimo=new HashMap<>(); for(TitleEntry t:titulos)porAcronimo.put(normalizarAcronimo(t.acronimo()),t);
        for(ApcEntry tarifa:tarifas){
            TitleEntry titulo=porAcronimo.get(normalizarAcronimo(tarifa.acronimo()));
            if(titulo==null)titulo=mejorTitulo(tarifa,titulos);
            if(titulo==null){rechazados++;continue;}
            Optional<IeeeRevista> existente=repository.findFirstByAcronimoIgnoreCase(titulo.acronimo());
            IeeeRevista revista=existente.orElseGet(IeeeRevista::new); revista.setAcronimo(titulo.acronimo()); revista.setTitulo(titulo.titulo());
            asignarIssn(revista,titulo.issn(),false); asignarIssn(revista,titulo.eissn(),true); revista.setTipoAcceso(tarifa.tipoAcceso());
            revista.setApcUsd(quitarMoneda(tarifa.apc())); revista.setCargoSobreextension(tarifa.sobreextension());
            revista.setTarifaLicenciaRepositorio(quitarMoneda(tarifa.repositorio())); revista.setUrlOficial(sourceUrl);
            revista.setFuenteArchivo("IEEE-Article-Processing-Charges-List.pdf + IEEE-Title-List-January-2026.pdf"); repository.save(revista);
            if(existente.isEmpty())insertados++;else actualizados++;
        }
        repository.flush(); return new CatalogImportResponseDTO(tarifas.size(),insertados,actualizados,rechazados,(System.nanoTime()-inicio)/1_000_000);
    }

    List<TitleEntry> parsearTitulos(byte[] pdf){
        String texto=extraer(pdf).replace('\u00a0',' ').replaceAll("\\s+"," "); List<TitleEntry> salida=new ArrayList<>(); Matcher m=TITLE_ROW.matcher(texto);
        while(m.find()){String titulo=limpiar(m.group(2)); if(titulo==null||titulo.contains("Publication Title"))continue; salida.add(new TitleEntry(m.group(1),titulo,m.group(3)+" Open Access",nuloNA(m.group(4)),nuloNA(m.group(5))));}
        return salida;
    }

    List<ApcEntry> parsearTarifas(byte[] pdf){
        List<ApcEntry> salida=new ArrayList<>(); StringBuilder pendiente=new StringBuilder();
        for(String cruda:extraer(pdf).replace('\u00a0',' ').split("\\R")){String linea=limpiar(cruda);if(linea==null||esRuido(linea))continue;Matcher fila=APC_ROW.matcher(linea);
            if(!fila.matches()){if(!linea.startsWith("$")&&!linea.matches(".*\\d{1,2}/\\d{1,2}.*")){if(pendiente.length()>0)pendiente.append(' ');pendiente.append(linea);}continue;}
            String titulo=limpiar((pendiente+" "+fila.group(1)).replaceAll("\\s+"," "));pendiente.setLength(0);if(titulo==null)continue;
            String acronimo=fila.group(2); if(acronimo.startsWith("IEEE")&&acronimo.length()>4){acronimo=acronimo.substring(4);titulo=titulo+" IEEE";}
            List<String> importes=new ArrayList<>();Matcher dinero=MONEY.matcher((fila.group(4)==null?"":fila.group(4))+fila.group(5));while(dinero.find())importes.add(dinero.group());
            String apc=fila.group(4);String sobre=importes.size()>1&&!"$1,275".equals(importes.get(1))?importes.get(1):null;String repo=null;for(String x:importes)if("$1,275".equals(x))repo=x;
            salida.add(new ApcEntry(acronimo,titulo,fila.group(3),apc,sobre,repo));
        }return salida;
    }

    private TitleEntry mejorTitulo(ApcEntry apc,List<TitleEntry> titulos){TitleEntry mejor=null;double puntaje=0;for(TitleEntry t:titulos){double p=similitud(apc.titulo(),t.titulo());if(p>puntaje){puntaje=p;mejor=t;}}return puntaje>=0.55?mejor:null;}
    private double similitud(String a,String b){Set<String>x=tokens(a),y=tokens(b);if(x.isEmpty()||y.isEmpty())return 0;Set<String>i=new HashSet<>(x);i.retainAll(y);Set<String>u=new HashSet<>(x);u.addAll(y);return (double)i.size()/u.size();}
    private Set<String> tokens(String v){Set<String>s=new HashSet<>(Arrays.asList(normalizarTexto(v).split(" ")));s.removeAll(NOISE);s.removeIf(String::isBlank);return s;}
    private boolean esRuido(String l){String n=normalizarTexto(l);return n.startsWith("2026 ieee publications")||n.startsWith("number of pages")||n.equals("repository licensing fee")||n.equals("title acronym")||n.startsWith("oa pub type")||n.startsWith("per page")||n.startsWith("reference bio")||n.matches("regular|brief|letter|invited|overview|review|survey|tutorials|paper|method|state|of the|art.*|clinical.*|ieee conference.*|symposia.*|special.*|issues");}
    private String extraer(byte[] pdf){try(PdfReader reader=new PdfReader(pdf)){PdfTextExtractor extractor=new PdfTextExtractor(reader);StringBuilder s=new StringBuilder();for(int p=1;p<=reader.getNumberOfPages();p++)s.append(extractor.getTextFromPage(p)).append('\n');return s.toString();}catch(IOException ex){throw new IllegalArgumentException("No fue posible leer el PDF oficial de IEEE: "+mensaje(ex),ex);}}
    private byte[] descargar(URI url)throws IOException,InterruptedException{HttpRequest q=HttpRequest.newBuilder(url).timeout(timeout).header("Accept",PDF).header("User-Agent","SICREC-IEEE-APC/1.0").GET().build();HttpResponse<byte[]>r=http.send(q,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()<200||r.statusCode()>=300)throw new IOException("IEEE respondió con HTTP "+r.statusCode());byte[]b=r.body();if(b==null||b.length<5||b[0]!='%'||b[1]!='P'||b[2]!='D'||b[3]!='F')throw new IOException("IEEE no devolvió un PDF válido");return b;}
    private byte[] concatenar(byte[]a,byte[]b){byte[]r=Arrays.copyOf(a,a.length+b.length);System.arraycopy(b,0,r,a.length,b.length);return r;}
    private void asignarIssn(IeeeRevista r,String valor,boolean electronico){String n=normalizarIssn(valor);String f=n==null?null:n.substring(0,4)+"-"+n.substring(4);if(electronico){r.setEissn(f);r.setEissnNormalizado(n);}else{r.setIssn(f);r.setIssnNormalizado(n);}}
    private JournalInfo convertir(IeeeRevista r){Map<String,String>d=new LinkedHashMap<>();poner(d,"tipoAcceso",r.getTipoAcceso());poner(d,"apcUsd",r.getApcUsd());poner(d,"cargoSobreextension",r.getCargoSobreextension());poner(d,"tarifaLicenciaRepositorio",r.getTarifaLicenciaRepositorio());poner(d,"descuentoMiembroIEEE","5%");poner(d,"descuentoMiembroSociedadIEEE","20%");poner(d,"openAccess",r.getTipoAcceso()!=null&&r.getTipoAcceso().startsWith("Full")?"Sí":"Opcional");return new JournalInfo(nombre(),r.getTitulo(),r.getIssn(),r.getEissn(),"IEEE",null,null,null,"CC BY / CC BY-NC-ND",r.getApcUsd(),r.getApcUsd()==null?null:"USD",r.getUrlOficial(),d);}
    private String normalizarIssn(String v){if(v==null)return null;String n=v.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]","");return n.matches("\\d{7}[\\dX]")?n:null;}
    private String normalizarAcronimo(String v){return v==null?"":v.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");}
    private String normalizarTexto(String v){if(v==null)return "";return Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    private String nuloNA(String v){return v==null||"N/A".equalsIgnoreCase(v)?null:v;} private String quitarMoneda(String v){return v==null?null:v.replace("$","").replace(",","");}
    private String limpiar(String v){return v==null||v.isBlank()?null:v.trim();} private String mensaje(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private void poner(Map<String,String>d,String k,String v){if(v!=null)d.put(k,v);} private void auditar(boolean ok,String d){try{if(ok)auditoria.registrarExito(null,"IEEE","IMPORTACIÓN_APC_AUTOMÁTICA",d);else auditoria.registrarError(null,"IEEE","IMPORTACIÓN_APC_AUTOMÁTICA",d);}catch(RuntimeException ex){LOG.error("No se pudo registrar la auditoría IEEE",ex);}}
    record TitleEntry(String acronimo,String titulo,String tipoAcceso,String issn,String eissn){} record ApcEntry(String acronimo,String titulo,String tipoAcceso,String apc,String sobreextension,String repositorio){}
}
