package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// INICIO - Descarga automática PDF Springer
class SpringerPdfDownloadServiceTests {

    private static final byte[] PDF_FULLY = "%PDF-fully".getBytes();
    private static final byte[] PDF_HYBRID = "%PDF-hybrid".getBytes();

    @TempDir
    Path temporal;

    private SpringerPdfImportService importService;
    private AuditoriaService auditoriaService;
    private HttpClient httpClient;

    @BeforeEach
    void configurar() {
        importService = mock(SpringerPdfImportService.class);
        auditoriaService = mock(AuditoriaService.class);
        httpClient = mock(HttpClient.class);
    }

    @Test
    void descargaCorrectamenteFullyOaYLuegoHybrid() throws Exception {
        responderPorUrl(PDF_FULLY, PDF_HYBRID);
        List<String> nombres = new ArrayList<>();
        doAnswer(invocacion -> {
            MultipartFile archivo = invocacion.getArgument(0);
            nombres.add(archivo.getOriginalFilename());
            return null;
        }).when(importService).importar(any());

        servicio().descargarEImportar();

        assertThat(nombres).containsExactly(
                "fully.pdf",
                "hybrid.pdf"
        );
        verify(auditoriaService).registrarExito(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_FULLY_OA"), any()
        );
        verify(auditoriaService).registrarExito(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_HYBRID"), any()
        );
    }

    @Test
    void noImportaArchivosSinCambios() throws Exception {
        responderPorUrl(PDF_FULLY, PDF_HYBRID);
        SpringerPdfDownloadService servicio = servicio();

        servicio.descargarEImportar();
        servicio.descargarEImportar();

        verify(importService, times(2)).importar(any());
    }

    @Test
    void importaNuevamenteCuandoCambiaElHash() throws Exception {
        AtomicInteger fullyRequests = new AtomicInteger();
        when(httpClient.send(any(), any())).thenAnswer(invocacion -> {
            HttpRequest request = invocacion.getArgument(0);
            if (request.uri().getPath().contains("fully")) {
                return respuesta(
                        200,
                        fullyRequests.getAndIncrement() == 0
                                ? PDF_FULLY
                                : "%PDF-fully-cambiado".getBytes()
                );
            }
            return respuesta(200, PDF_HYBRID);
        });
        SpringerPdfDownloadService servicio = servicio();

        servicio.descargarEImportar();
        servicio.descargarEImportar();

        verify(importService, times(3)).importar(any());
    }

    @Test
    void rechazaPdfInvalido() throws Exception {
        responderPorUrl("<html>Error</html>".getBytes(), PDF_HYBRID);

        servicio().descargarEImportar();

        verify(importService, times(1)).importar(any());
        verify(auditoriaService).registrarError(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_FULLY_OA"), any()
        );
    }

    @Test
    void manejaErrorHttp() throws Exception {
        when(httpClient.send(any(), any())).thenAnswer(invocacion -> {
            HttpRequest request = invocacion.getArgument(0);
            return request.uri().getPath().contains("fully")
                    ? respuesta(503, new byte[0])
                    : respuesta(200, PDF_HYBRID);
        });

        servicio().descargarEImportar();

        verify(importService, times(1)).importar(any());
        verify(auditoriaService).registrarError(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_FULLY_OA"), any()
        );
    }

    @Test
    void manejaTimeout() throws Exception {
        when(httpClient.send(any(), any())).thenAnswer(invocacion -> {
            HttpRequest request = invocacion.getArgument(0);
            if (request.uri().getPath().contains("fully")) {
                throw new HttpTimeoutException("timeout de prueba");
            }
            return respuesta(200, PDF_HYBRID);
        });

        servicio().descargarEImportar();

        verify(importService, times(1)).importar(any());
        verify(auditoriaService).registrarError(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_FULLY_OA"), any()
        );
    }

    @Test
    void noGuardaHashCuandoFallaLaImportacion() throws Exception {
        responderPorUrl(PDF_FULLY, PDF_HYBRID);
        when(importService.importar(any())).thenAnswer(invocacion -> {
            MultipartFile archivo = invocacion.getArgument(0);
            if ("fully.pdf".equals(archivo.getOriginalFilename())) {
                throw new IllegalStateException("fallo importación");
            }
            return null;
        });

        servicio().descargarEImportar();
        servicio().descargarEImportar();

        verify(importService, times(3)).importar(any());
        verify(auditoriaService, times(2)).registrarError(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_FULLY_OA"), any()
        );
    }

    @Test
    void continuaConHybridCuandoFullyOaFalla() throws Exception {
        responderPorUrl(PDF_FULLY, PDF_HYBRID);
        List<String> intentos = new ArrayList<>();
        when(importService.importar(any())).thenAnswer(invocacion -> {
            MultipartFile archivo = invocacion.getArgument(0);
            intentos.add(archivo.getOriginalFilename());
            if ("fully.pdf".equals(archivo.getOriginalFilename())) {
                throw new IllegalStateException("fallo Fully OA");
            }
            return null;
        });

        servicio().descargarEImportar();

        assertThat(intentos).containsExactly("fully.pdf", "hybrid.pdf");
        verify(auditoriaService).registrarExito(
                eq(null), eq("SPRINGER"),
                eq("IMPORTACIÓN_APC_AUTOMÁTICA_HYBRID"), any()
        );
    }

    private SpringerPdfDownloadService servicio() {
        return new SpringerPdfDownloadService(
                importService,
                auditoriaService,
                httpClient,
                Duration.ofMillis(100),
                new SpringerPdfDownloadService.FuentePdf(
                        "FULLY_OA",
                        URI.create("https://springer.test/fully"),
                        temporal.resolve("fully.sha256"),
                        "fully.pdf"
                ),
                new SpringerPdfDownloadService.FuentePdf(
                        "HYBRID",
                        URI.create("https://springer.test/hybrid"),
                        temporal.resolve("hybrid.sha256"),
                        "hybrid.pdf"
                ),
                true
        );
    }

    private void responderPorUrl(byte[] fully, byte[] hybrid) throws Exception {
        when(httpClient.send(any(), any())).thenAnswer(invocacion -> {
            HttpRequest request = invocacion.getArgument(0);
            return respuesta(
                    200,
                    request.uri().getPath().contains("fully") ? fully : hybrid
            );
        });
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> respuesta(int estado, byte[] contenido) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(estado);
        when(response.body()).thenReturn(contenido);
        return response;
    }
}
// FIN - Descarga automática PDF Springer
