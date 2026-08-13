package uteq.edu.ec.crecuteq.provider;

import com.sun.net.httpserver.HttpServer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.entity.WileyRevista;
import uteq.edu.ec.crecuteq.repository.WileyRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WileyProviderTests {
    @TempDir Path temporal;

    @Test
    void importaLaListaFullyOpenAccess() throws Exception {
        WileyRevistaRepository repository = vacio();
        var resultado = provider(repository).importar(excel(false));
        assertEquals(1, resultado.getRegistrosInsertados());
        verify(repository).save(argThat(r -> "Fully Open Access".equals(r.getModeloPublicacion())
                && "1240".equals(r.getApcUsd()) && "13 July 2026".equals(r.getVigencia())));
    }

    @Test
    void importaLaListaHybridConEncabezadosVariantes() throws Exception {
        WileyRevistaRepository repository = vacio();
        var resultado = provider(repository).importar(excel(true));
        assertEquals(1, resultado.getRegistrosInsertados());
        verify(repository).save(argThat(r -> r.getModeloPublicacion().startsWith("Hybrid")
                && "3570".equals(r.getApcUsd()) && "2990".equals(r.getApcEur())));
    }

    @Test
    void buscaPrimeroPorEissnYExponeTodosLosDatos() {
        WileyRevista revista = new WileyRevista();
        revista.setTitulo("Abacus"); revista.setOnlineIssn("1467-6281");
        revista.setModeloPublicacion("Hybrid Open Access (OnlineOpen)");
        revista.setAreaTematica("Accounting"); revista.setLicencias("CC BY");
        revista.setApcUsd("3570"); revista.setApcGbp("2380"); revista.setApcEur("2990");
        WileyRevistaRepository repository = mock(WileyRevistaRepository.class);
        when(repository.findFirstByOnlineIssnNormalizado("14676281")).thenReturn(Optional.of(revista));
        var info = provider(repository).buscar(null, null, "1467-6281").orElseThrow();
        assertEquals("wiley", info.getProveedor());
        assertEquals("1467-6281", info.getEissn());
        assertEquals("2990", info.getDatosAdicionales().get("apcEur"));
    }

    @Test
    void schedulerImportaAmbasListasUnaSolaVezSiNoCambian() throws Exception {
        byte[] open = excel(false).getBytes();
        byte[] hybrid = excel(true).getBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/open.xlsx", exchange -> responder(exchange, open));
        server.createContext("/hybrid.xlsx", exchange -> responder(exchange, hybrid));
        server.start();
        try {
            WileyRevistaRepository repository = vacio();
            AuditoriaService auditoria = mock(AuditoriaService.class);
            String base = "http://localhost:" + server.getAddress().getPort();
            WileyProvider provider = new WileyProvider(repository, auditoria, HttpClient.newHttpClient(),
                    URI.create(base + "/open.xlsx"), URI.create(base + "/hybrid.xlsx"),
                    Duration.ofSeconds(2), temporal.resolve("open.sha256"),
                    temporal.resolve("hybrid.sha256"), true);

            provider.descargarEImportar();
            provider.descargarEImportar();

            verify(repository, times(2)).save(any(WileyRevista.class));
            assertTrue(java.nio.file.Files.exists(temporal.resolve("open.sha256")));
            assertTrue(java.nio.file.Files.exists(temporal.resolve("hybrid.sha256")));
        } finally { server.stop(0); }
    }

    private void responder(com.sun.net.httpserver.HttpExchange exchange, byte[] body) throws java.io.IOException {
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private WileyRevistaRepository vacio() {
        WileyRevistaRepository repository = mock(WileyRevistaRepository.class);
        when(repository.findFirstByOnlineIssnNormalizado(any())).thenReturn(Optional.empty());
        return repository;
    }

    private WileyProvider provider(WileyRevistaRepository repository) {
        return new WileyProvider(repository, mock(AuditoriaService.class), HttpClient.newHttpClient(),
                URI.create("https://wiley.invalid/open.xlsx"), URI.create("https://wiley.invalid/hybrid.xlsx"),
                Duration.ofSeconds(1), temporal.resolve("open.sha256"), temporal.resolve("hybrid.sha256"), false);
    }

    private MockMultipartFile excel(boolean hybrid) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("APCs");
            sheet.createRow(0).createCell(0).setCellValue(hybrid
                    ? "Wiley Article Publication Charges for Hybrid Open Access Journals"
                    : "Wiley Open Access Journals Licensing and APCs");
            sheet.createRow(1).createCell(0).setCellValue("Updated: " + (hybrid ? "14 July 2026" : "13 July 2026"));
            var header = sheet.createRow(5);
            String[] columnas = hybrid
                    ? new String[]{"Journal Title", "Subject Area", "Online\nISSN", "License types offered", "USD $", "GBP £", "EUR €"}
                    : new String[]{"Journal Name", "Subject Area", "Online ISSN", "License Types Offered", "USD", "GBP", "EUR"};
            for (int i = 0; i < columnas.length; i++) header.createCell(i).setCellValue(columnas[i]);
            var row = sheet.createRow(6);
            row.createCell(0).setCellValue(hybrid ? "Abacus" : "Abstract and Applied Analysis");
            row.createCell(1).setCellValue(hybrid ? "Accounting" : "Mathematics");
            row.createCell(2).setCellValue(hybrid ? "14676281" : "16870409");
            row.createCell(3).setCellValue("CC BY"); row.createCell(4).setCellValue(hybrid ? 3570 : 1240);
            row.createCell(5).setCellValue(hybrid ? 2380 : 920); row.createCell(6).setCellValue(hybrid ? 2990 : 1080);
            workbook.write(output);
            return new MockMultipartFile("archivo", hybrid ? "Wiley-OnlineOpen.xlsx" : "Wiley-Open-Access.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
