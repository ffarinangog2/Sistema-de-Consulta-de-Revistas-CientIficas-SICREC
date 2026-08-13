package uteq.edu.ec.crecuteq.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.entity.Scimago;
import uteq.edu.ec.crecuteq.repository.ScimagoRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScimagoProviderTests {
    private static final String CSV = "Sourceid;Title;Type;Issn;SJR;SJR Best Quartile;Country;Region;Coverage;Categories;Areas\n"
            + "123;Revista Uno;journal;1234-5678, 2049-3630;1,25;Q1;Ecuador;Latin America;2020-2025;Medicine;Health\n";

    @TempDir Path temporal;

    @Test
    void importaCsvOficialSeparadoPorPuntoYComa() {
        ScimagoRepository repository = mock(ScimagoRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        ScimagoProvider provider = provider(repository, mock(AuditoriaService.class));

        CatalogImportResponseDTO resultado = provider.importar(archivo(CSV));

        assertEquals(1, resultado.getRegistrosLeidos());
        assertEquals(1, resultado.getRegistrosInsertados());
        verify(repository).saveAllAndFlush(anyList());
    }

    @Test
    void actualizaSoloCamposDelCatalogoYConservaDatosLegados() {
        Scimago existente = new Scimago();
        existente.setSourceid(123d);
        existente.setPublisher23("Editorial conservada");
        ScimagoRepository repository = mock(ScimagoRepository.class);
        when(repository.findAll()).thenReturn(List.of(existente));

        CatalogImportResponseDTO resultado = provider(repository, mock(AuditoriaService.class))
                .importar(archivo(CSV));

        assertEquals(1, resultado.getRegistrosActualizados());
        assertEquals("Q1", existente.getSJRBestQuartile());
        assertEquals("Ecuador", existente.getCountry());
        assertEquals("Editorial conservada", existente.getPublisher23());
    }

    @Test
    void nuncaDevuelveScimagoComoRevistaIndependiente() {
        assertTrue(provider(mock(ScimagoRepository.class), mock(AuditoriaService.class))
                .buscar(null, "1234-5678", null).isEmpty());
    }

    @Test
    void descargaEImportaSoloCuandoCambiaElCatalogo() throws Exception {
        byte[] contenido = CSV.getBytes(StandardCharsets.UTF_8);
        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/catalogo", intercambio -> {
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
            intercambio.close();
        });
        servidor.start();
        try {
            ScimagoRepository repository = mock(ScimagoRepository.class);
            when(repository.findAll()).thenReturn(List.of());
            ScimagoProvider provider = new ScimagoProvider(repository,
                    mock(AuditoriaService.class), HttpClient.newHttpClient(),
                    URI.create("http://localhost:" + servidor.getAddress().getPort() + "/catalogo"),
                    Duration.ofSeconds(2), temporal.resolve("scimago.sha256"), true);

            provider.descargarEImportar();
            provider.descargarEImportar();

            verify(repository, times(1)).saveAllAndFlush(anyList());
        } finally {
            servidor.stop(0);
        }
    }

    private ScimagoProvider provider(ScimagoRepository repository, AuditoriaService auditoria) {
        return new ScimagoProvider(repository, auditoria, HttpClient.newHttpClient(),
                URI.create("https://scimago.invalid/catalogo"), Duration.ofSeconds(1),
                temporal.resolve("unused.sha256"), false);
    }

    private MockMultipartFile archivo(String contenido) {
        return new MockMultipartFile("archivo", "scimago.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }
}
