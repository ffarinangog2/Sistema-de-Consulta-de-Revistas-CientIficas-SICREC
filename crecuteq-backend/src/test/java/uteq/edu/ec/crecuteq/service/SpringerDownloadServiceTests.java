package uteq.edu.ec.crecuteq.service;

import com.sun.net.httpserver.HttpServer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

// INICIO - Descarga automática Springer
class SpringerDownloadServiceTests {

    @TempDir
    Path temporal;

    @Test
    void importaUnaSolaVezCuandoElCatalogoNoCambia() throws Exception {
        byte[] xlsx = crearXlsx();
        HttpServer servidor = servidor(200, xlsx);
        try {
            SpringerImportService importService = mock(SpringerImportService.class);
            AuditoriaService auditoriaService = mock(AuditoriaService.class);
            SpringerDownloadService service = servicio(
                    servidor,
                    importService,
                    auditoriaService
            );

            service.descargarEImportar();
            service.descargarEImportar();

            verify(importService, times(1)).importar(any());
            verify(auditoriaService, times(2)).registrarExito(
                    any(), any(), any(), any()
            );
            verify(auditoriaService, never()).registrarError(
                    any(), any(), any(), any()
            );
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void rechazaUnaRespuestaQueNoEsXlsxSinInvocarImportacion()
            throws Exception {
        HttpServer servidor = servidor(200, "<html>Error</html>".getBytes());
        try {
            SpringerImportService importService = mock(SpringerImportService.class);
            AuditoriaService auditoriaService = mock(AuditoriaService.class);

            servicio(servidor, importService, auditoriaService)
                    .descargarEImportar();

            verify(importService, never()).importar(any());
            verify(auditoriaService).registrarError(
                    any(), any(), any(), any()
            );
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void rechazaCodigoHttpNoExitoso() throws Exception {
        HttpServer servidor = servidor(503, new byte[0]);
        try {
            SpringerImportService importService = mock(SpringerImportService.class);
            AuditoriaService auditoriaService = mock(AuditoriaService.class);

            servicio(servidor, importService, auditoriaService)
                    .descargarEImportar();

            verify(importService, never()).importar(any());
            verify(auditoriaService).registrarError(
                    any(), any(), any(), any()
            );
        } finally {
            servidor.stop(0);
        }
    }

    private SpringerDownloadService servicio(
            HttpServer servidor,
            SpringerImportService importService,
            AuditoriaService auditoriaService
    ) {
        URI url = URI.create(
                "http://localhost:" + servidor.getAddress().getPort() + "/catalogo"
        );
        return new SpringerDownloadService(
                importService,
                auditoriaService,
                HttpClient.newHttpClient(),
                url,
                Duration.ofSeconds(2),
                temporal.resolve("springer.sha256"),
                true
        );
    }

    private HttpServer servidor(int estado, byte[] respuesta) throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/catalogo", exchange -> {
            exchange.sendResponseHeaders(estado, respuesta.length);
            exchange.getResponseBody().write(respuesta);
            exchange.close();
        });
        servidor.start();
        return servidor;
    }

    private byte[] crearXlsx() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            workbook.createSheet("Catalog");
            workbook.write(salida);
            return salida.toByteArray();
        }
    }
}
// FIN - Descarga automática Springer
