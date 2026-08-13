package uteq.edu.ec.crecuteq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.etl.CatalogDownloadSupport;
import uteq.edu.ec.crecuteq.etl.InMemoryMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// INICIO - Descarga automática Springer
@Service
public class SpringerDownloadService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringerDownloadService.class);
    private static final String MODULO = "SPRINGER";
    private static final String ACCION = "IMPORTACIÓN_AUTOMÁTICA";
    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SpringerImportService springerImportService;
    private final AuditoriaService auditoriaService;
    private final HttpClient httpClient;
    private final URI catalogoUrl;
    private final Duration readTimeout;
    private final Path hashPath;
    private final boolean habilitado;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @Autowired
    public SpringerDownloadService(
            SpringerImportService springerImportService,
            AuditoriaService auditoriaService,
            @Value("${springer.download.url}") String catalogoUrl,
            @Value("${springer.download.connect-timeout-ms:10000}")
            long connectTimeoutMs,
            @Value("${springer.download.read-timeout-ms:30000}")
            long readTimeoutMs,
            @Value("${springer.download.hash-file:data/springer-catalog.sha256}")
            String hashFile,
            @Value("${springer.download.enabled:true}") boolean habilitado
    ) {
        this(
                springerImportService,
                auditoriaService,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                URI.create(catalogoUrl),
                Duration.ofMillis(readTimeoutMs),
                Path.of(hashFile),
                habilitado
        );
    }

    SpringerDownloadService(
            SpringerImportService springerImportService,
            AuditoriaService auditoriaService,
            HttpClient httpClient,
            URI catalogoUrl,
            Duration readTimeout,
            Path hashPath,
            boolean habilitado
    ) {
        this.springerImportService = springerImportService;
        this.auditoriaService = auditoriaService;
        this.httpClient = httpClient;
        this.catalogoUrl = catalogoUrl;
        this.readTimeout = readTimeout;
        this.hashPath = hashPath;
        this.habilitado = habilitado;
    }

    @Scheduled(cron = "${springer.download.cron:0 0 1 1 * *}")
    public void descargarEImportar() {
        if (!habilitado || !ejecutando.compareAndSet(false, true)) {
            return;
        }

        try {
            byte[] archivo = descargar();
            validarXlsx(archivo);
            String hash = CatalogDownloadSupport.calcularSha256(archivo);

            if (hash.equals(CatalogDownloadSupport.leerUltimoHash(hashPath))) {
                registrarExitoSeguro(
                        "Catálogo oficial sin cambios; importación omitida. SHA-256: "
                                + hash
                );
                return;
            }

            springerImportService.importar(new InMemoryMultipartFile(
                    archivo,
                    "springer-nature-journals-catalog.xlsx",
                    CONTENT_TYPE_XLSX
            ));
            CatalogDownloadSupport.guardarHash(
                    hashPath, hash, "springer-catalog-");
            registrarExitoSeguro(
                    "Catálogo oficial descargado e importado. SHA-256: " + hash
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            registrarErrorSeguro("Descarga interrumpida");
        } catch (Exception exception) {
            LOGGER.error(
                    "No fue posible descargar o importar el catálogo Springer",
                    exception
            );
            registrarErrorSeguro(
                    "No fue posible descargar o importar el catálogo oficial: "
                            + mensaje(exception)
            );
        } finally {
            ejecutando.set(false);
        }
    }

    private byte[] descargar() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(catalogoUrl)
                .timeout(readTimeout)
                .header("Accept", CONTENT_TYPE_XLSX)
                .header("User-Agent", "CRECUTEQ-Springer-Catalog/1.0")
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

    private void validarXlsx(byte[] archivo) throws IOException {
        if (archivo == null || archivo.length < 4
                || archivo[0] != 'P' || archivo[1] != 'K') {
            throw new IOException("La respuesta no tiene la firma de un XLSX");
        }

        boolean contentTypes = false;
        boolean workbook = false;
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(archivo))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                contentTypes |= "[Content_Types].xml".equals(entrada.getName());
                workbook |= "xl/workbook.xml".equals(entrada.getName());
            }
        }

        if (!contentTypes || !workbook) {
            throw new IOException(
                    "La respuesta no contiene la estructura requerida de un XLSX"
            );
        }
    }

    private void registrarExitoSeguro(String descripcion) {
        try {
            auditoriaService.registrarExito(null, MODULO, ACCION, descripcion);
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible registrar la auditoría Springer", exception);
        }
    }

    private void registrarErrorSeguro(String descripcion) {
        try {
            auditoriaService.registrarError(null, MODULO, ACCION, descripcion);
        } catch (RuntimeException exception) {
            LOGGER.error("No fue posible registrar la auditoría Springer", exception);
        }
    }

    private String mensaje(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

}
// FIN - Descarga automática Springer
