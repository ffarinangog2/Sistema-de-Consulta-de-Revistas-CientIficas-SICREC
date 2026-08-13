package uteq.edu.ec.crecuteq.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.DoajRevista;
import uteq.edu.ec.crecuteq.repository.DoajRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// INICIO - Pruebas de importación, búsqueda y actualización DOAJ
class DoajProviderTests {

    private static final String ENCABEZADOS =
            "Journal title,Journal ISSN (print version),"
                    + "Journal EISSN (online version),Publisher,"
                    + "Country of publisher,Journal URL\n";

    @TempDir
    Path temporal;

    @Test
    void importaEInsertaUnRegistroDelCsvOficial() {
        DoajRevistaRepository repository = mock(DoajRevistaRepository.class);
        AuditoriaService auditoria = mock(AuditoriaService.class);
        when(repository.findFirstByIssnNormalizado(any()))
                .thenReturn(Optional.empty());
        when(repository.findFirstByEissnNormalizado(any()))
                .thenReturn(Optional.empty());

        CatalogImportResponseDTO resultado = provider(repository, auditoria)
                .importar(csv("Revista Uno,1234-5678,2049-3630,UTEQ,Ecuador,"
                        + "https://example.org\n"));

        assertEquals(1, resultado.getRegistrosLeidos());
        assertEquals(1, resultado.getRegistrosInsertados());
        assertEquals(0, resultado.getRegistrosActualizados());
        verify(repository).saveAndFlush(any(DoajRevista.class));
    }

    @Test
    void buscaPorEissnYDevuelveElDtoComun() {
        DoajRevista revista = new DoajRevista();
        revista.setTitulo("Revista DOAJ");
        revista.setIssn("1234-5678");
        revista.setEissn("2049-3630");
        revista.setEditorial("UTEQ");
        revista.setDatosCsv("{\"Journal title\":\"Revista DOAJ\"}");

        DoajRevistaRepository repository = mock(DoajRevistaRepository.class);
        when(repository.findFirstByEissnNormalizado("20493630"))
                .thenReturn(Optional.of(revista));

        Optional<JournalInfo> resultado = provider(
                repository, mock(AuditoriaService.class))
                .buscar(null, null, "2049-3630");

        assertTrue(resultado.isPresent());
        assertEquals("doaj", resultado.orElseThrow().getProveedor());
        assertNull(resultado.orElseThrow().getTitulo());
        assertEquals("2049-3630", resultado.orElseThrow().getEissn());
        assertNull(resultado.orElseThrow().getEditorial());
    }

    @Test
    void actualizaElRegistroCuandoElIssnYaExiste() {
        DoajRevista existente = new DoajRevista();
        existente.setTitulo("Título anterior");
        existente.setIssnNormalizado("12345678");
        existente.setDatosCsv("{}");

        DoajRevistaRepository repository = mock(DoajRevistaRepository.class);
        when(repository.findFirstByIssnNormalizado("12345678"))
                .thenReturn(Optional.of(existente));

        CatalogImportResponseDTO resultado = provider(
                repository, mock(AuditoriaService.class))
                .importar(csv("Título actualizado,1234-5678,,Editorial,EC,"
                        + "https://example.org\n"));

        assertEquals(0, resultado.getRegistrosInsertados());
        assertEquals(1, resultado.getRegistrosActualizados());
        assertEquals("Título actualizado", existente.getTitulo());
        verify(repository).saveAndFlush(existente);
    }

    @Test
    void descargaUnaSolaVezSiElCatalogoNoCambia() throws Exception {
        byte[] contenido = (ENCABEZADOS
                + "Revista Uno,1234-5678,2049-3630,UTEQ,Ecuador,"
                + "https://example.org\n").getBytes(StandardCharsets.UTF_8);
        HttpServer servidor = servidor(contenido);
        try {
            DoajRevistaRepository repository = mock(DoajRevistaRepository.class);
            when(repository.findFirstByIssnNormalizado(any()))
                    .thenReturn(Optional.empty());
            when(repository.findFirstByEissnNormalizado(any()))
                    .thenReturn(Optional.empty());
            AuditoriaService auditoria = mock(AuditoriaService.class);
            DoajProvider provider = new DoajProvider(
                    repository,
                    auditoria,
                    HttpClient.newHttpClient(),
                    URI.create("http://localhost:"
                            + servidor.getAddress().getPort() + "/catalogo"),
                    Duration.ofSeconds(2),
                    temporal.resolve("doaj.sha256"),
                    true);

            provider.descargarEImportar();
            provider.descargarEImportar();

            verify(repository, times(1)).saveAndFlush(any(DoajRevista.class));
        } finally {
            servidor.stop(0);
        }
    }

    private DoajProvider provider(
            DoajRevistaRepository repository, AuditoriaService auditoria
    ) {
        return new DoajProvider(
                repository, auditoria, HttpClient.newHttpClient(),
                URI.create("https://doaj.invalid/csv"), Duration.ofSeconds(1),
                temporal.resolve("unused.sha256"), false);
    }

    private MockMultipartFile csv(String fila) {
        return new MockMultipartFile(
                "archivo", "doaj.csv", "text/csv",
                (ENCABEZADOS + fila).getBytes(StandardCharsets.UTF_8));
    }

    private HttpServer servidor(byte[] respuesta) throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/catalogo", exchange -> {
            exchange.sendResponseHeaders(200, respuesta.length);
            exchange.getResponseBody().write(respuesta);
            exchange.close();
        });
        servidor.start();
        return servidor;
    }
}
// FIN - Pruebas de importación, búsqueda y actualización DOAJ
