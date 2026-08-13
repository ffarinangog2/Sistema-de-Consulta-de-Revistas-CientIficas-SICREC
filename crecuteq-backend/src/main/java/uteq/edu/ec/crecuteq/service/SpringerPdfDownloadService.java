package uteq.edu.ec.crecuteq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

// INICIO - Descarga automática PDF Springer
@Service
public class SpringerPdfDownloadService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringerPdfDownloadService.class);
    private static final String MODULO = "SPRINGER";
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final SpringerPdfImportService importService;
    private final AuditoriaService auditoriaService;
    private final HttpClient httpClient;
    private final Duration readTimeout;
    private final FuentePdf fullyOa;
    private final FuentePdf hybrid;
    private final boolean habilitado;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @Autowired
    public SpringerPdfDownloadService(
            SpringerPdfImportService importService,
            AuditoriaService auditoriaService,
            @Value("${springer.pdf-download.fully-oa.url}") String fullyOaUrl,
            @Value("${springer.pdf-download.hybrid.url}") String hybridUrl,
            @Value("${springer.pdf-download.connect-timeout-ms:10000}")
            long connectTimeoutMs,
            @Value("${springer.pdf-download.read-timeout-ms:30000}")
            long readTimeoutMs,
            @Value("${springer.pdf-download.fully-oa.hash-file:"
                    + "data/springer-fully-oa.sha256}")
            String fullyOaHashFile,
            @Value("${springer.pdf-download.hybrid.hash-file:"
                    + "data/springer-hybrid.sha256}")
            String hybridHashFile,
            @Value("${springer.pdf-download.enabled:true}") boolean habilitado
    ) {
        this(
                importService,
                auditoriaService,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                Duration.ofMillis(readTimeoutMs),
                new FuentePdf(
                        "FULLY_OA",
                        URI.create(fullyOaUrl),
                        Path.of(fullyOaHashFile),
                        "springer-nature-fully-oa-apc.pdf"
                ),
                new FuentePdf(
                        "HYBRID",
                        URI.create(hybridUrl),
                        Path.of(hybridHashFile),
                        "springer-nature-hybrid-apc.pdf"
                ),
                habilitado
        );
    }

    SpringerPdfDownloadService(
            SpringerPdfImportService importService,
            AuditoriaService auditoriaService,
            HttpClient httpClient,
            Duration readTimeout,
            FuentePdf fullyOa,
            FuentePdf hybrid,
            boolean habilitado
    ) {
        this.importService = importService;
        this.auditoriaService = auditoriaService;
        this.httpClient = httpClient;
        this.readTimeout = readTimeout;
        this.fullyOa = fullyOa;
        this.hybrid = hybrid;
        this.habilitado = habilitado;
    }

    @Scheduled(cron = "${springer.pdf-download.cron:0 30 2 1 * *}")
    public void descargarEImportar() {
        if (!habilitado || !ejecutando.compareAndSet(false, true)) {
            return;
        }

        try {
            procesarIndependientemente(fullyOa);
            procesarIndependientemente(hybrid);
        } finally {
            ejecutando.set(false);
        }
    }

    private void procesarIndependientemente(FuentePdf fuente) {
        try {
            byte[] contenido = descargar(fuente.url());
            validarPdf(contenido);
            String hash = calcularSha256(contenido);

            if (hash.equals(leerHash(fuente.hashPath()))) {
                registrarExitoSeguro(
                        fuente,
                        "PDF oficial sin cambios; importación omitida. SHA-256: "
                                + hash
                );
                return;
            }

            importService.importar(
                    new ArchivoPdf(contenido, fuente.nombreArchivo())
            );
            guardarHash(fuente.hashPath(), hash);
            registrarExitoSeguro(
                    fuente,
                    "PDF oficial descargado e importado. SHA-256: " + hash
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            registrarErrorSeguro(fuente, "Descarga interrumpida");
        } catch (Exception exception) {
            LOGGER.error(
                    "Falló la descarga/importación PDF Springer {}",
                    fuente.tipo(),
                    exception
            );
            registrarErrorSeguro(
                    fuente,
                    "Falló la descarga o importación: " + mensaje(exception)
            );
        }
    }

    private byte[] descargar(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(url)
                .timeout(readTimeout)
                .header("Accept", CONTENT_TYPE_PDF)
                .header("User-Agent", "CRECUTEQ-Springer-APC/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Springer respondió con código HTTP " + response.statusCode()
            );
        }
        return response.body();
    }

    private void validarPdf(byte[] contenido) throws IOException {
        if (contenido == null || contenido.length < 5
                || contenido[0] != '%' || contenido[1] != 'P'
                || contenido[2] != 'D' || contenido[3] != 'F'
                || contenido[4] != '-') {
            throw new IOException("La respuesta no tiene una firma PDF válida");
        }
    }

    private String calcularSha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(contenido)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private String leerHash(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path).trim() : "";
    }

    private void guardarHash(Path path, String hash) throws IOException {
        Path absoluto = path.toAbsolutePath();
        Path directorio = absoluto.getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }
        Path temporal = Files.createTempFile(
                directorio,
                "springer-apc-",
                ".sha256.tmp"
        );
        try {
            Files.writeString(temporal, hash);
            try {
                Files.move(
                        temporal,
                        absoluto,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException exception) {
                Files.move(
                        temporal,
                        absoluto,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void registrarExitoSeguro(FuentePdf fuente, String descripcion) {
        try {
            auditoriaService.registrarExito(
                    null,
                    MODULO,
                    accion(fuente),
                    fuente.tipo() + " - " + descripcion
            );
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible auditar PDF {}", fuente.tipo(), exception);
        }
    }

    private void registrarErrorSeguro(FuentePdf fuente, String descripcion) {
        try {
            auditoriaService.registrarError(
                    null,
                    MODULO,
                    accion(fuente),
                    fuente.tipo() + " - " + descripcion
            );
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible auditar PDF {}", fuente.tipo(), exception);
        }
    }

    private String accion(FuentePdf fuente) {
        return "IMPORTACIÓN_APC_AUTOMÁTICA_" + fuente.tipo();
    }

    private String mensaje(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    record FuentePdf(
            String tipo,
            URI url,
            Path hashPath,
            String nombreArchivo
    ) {
    }

    private static final class ArchivoPdf implements MultipartFile {
        private final byte[] contenido;
        private final String nombre;

        private ArchivoPdf(byte[] contenido, String nombre) {
            this.contenido = contenido;
            this.nombre = nombre;
        }

        @Override public String getName() { return "archivo"; }
        @Override public String getOriginalFilename() { return nombre; }
        @Override public String getContentType() { return CONTENT_TYPE_PDF; }
        @Override public boolean isEmpty() { return contenido.length == 0; }
        @Override public long getSize() { return contenido.length; }
        @Override public byte[] getBytes() { return contenido.clone(); }
        @Override public InputStream getInputStream() {
            return new ByteArrayInputStream(contenido);
        }
        @Override public void transferTo(File destino) throws IOException {
            Files.write(destino.toPath(), contenido);
        }
    }
}
// FIN - Descarga automática PDF Springer
