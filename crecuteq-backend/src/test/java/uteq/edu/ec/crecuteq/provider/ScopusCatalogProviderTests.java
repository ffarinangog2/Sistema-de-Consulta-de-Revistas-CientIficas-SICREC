package uteq.edu.ec.crecuteq.provider;

import com.sun.net.httpserver.HttpServer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.entity.ScopusFuente;
import uteq.edu.ec.crecuteq.repository.ScopusFuenteRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScopusCatalogProviderTests {

    @TempDir
    Path tempDir;

    @Test
    void schedulerMensualOmiteReimportarSoloConHashYConteoCompletos() throws Exception {
        ScopusFuenteRepository repository = mock(ScopusFuenteRepository.class);
        when(repository.count()).thenReturn(0L, 1L, 1L);
        when(repository.findFirstByIssnNormalizado(anyString())).thenReturn(Optional.empty());
        when(repository.findFirstByEissnNormalizado(anyString())).thenReturn(Optional.empty());

        byte[] contenido = excelDiscontinuado().getBytes();
        AtomicInteger descargas = new AtomicInteger();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/scopus.xlsx", intercambio -> {
            descargas.incrementAndGet();
            intercambio.getResponseHeaders().add("Content-Type", XLSX);
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
            intercambio.close();
        });
        servidor.start();

        try {
            AuditoriaService auditoria = mock(AuditoriaService.class);
            ScopusCatalogProvider provider = new ScopusCatalogProvider(
                    repository, auditoria,
                    "http://localhost:" + servidor.getAddress().getPort() + "/scopus.xlsx",
                    1000, 5000, tempDir.resolve("scopus.sha256").toString(), true);

            provider.descargarEImportar();
            provider.descargarEImportar();

            assertThat(descargas).hasValue(2);
            ArgumentCaptor<ScopusFuente> captor = ArgumentCaptor.forClass(ScopusFuente.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().isDiscontinuada()).isTrue();
            verify(auditoria, times(2)).registrarExito(
                    isNull(), eq("SCOPUS_EXCEL"), eq("IMPORTACIÓN_AUTOMÁTICA"), anyString());
            verifyNoMoreInteractions(auditoria);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void importaSoloIdentificadoresEstadoYPeriodicidad() throws Exception {
        ScopusFuenteRepository repository = mock(ScopusFuenteRepository.class);
        when(repository.findFirstByIssnNormalizado(anyString())).thenReturn(Optional.empty());
        when(repository.findFirstByEissnNormalizado(anyString())).thenReturn(Optional.empty());

        var result = provider(repository).importar(excel());

        assertThat(result.getRegistrosLeidos()).isEqualTo(1);
        ArgumentCaptor<ScopusFuente> captor = ArgumentCaptor.forClass(ScopusFuente.class);
        verify(repository).save(captor.capture());
        ScopusFuente source = captor.getValue();
        assertThat(source.getIssn()).isEqualTo("1234-5678");
        assertThat(source.getEissn()).isEqualTo("2049-3630");
        assertThat(source.getEstado()).isEqualTo("Active");
        assertThat(source.isDiscontinuada()).isFalse();
        assertThat(source.getPeriodicidad()).isEqualTo("Monthly");
    }

    @Test
    void busquedaNoExponeTituloNiEditorial() {
        ScopusFuenteRepository repository = mock(ScopusFuenteRepository.class);
        ScopusFuente source = new ScopusFuente();
        source.setIssn("1234-5678");
        source.setEissn("2049-3630");
        source.setEstado("Inactive");
        source.setDiscontinuada(true);
        when(repository.findFirstByIssnNormalizado("12345678"))
                .thenReturn(Optional.of(source));

        var info = provider(repository)
                .buscar(null, "1234-5678", null).orElseThrow();

        assertThat(info.getTitulo()).isNull();
        assertThat(info.getEditorial()).isNull();
        assertThat(info.getIssn()).isEqualTo("1234-5678");
        assertThat(info.getDatosAdicionales()).containsEntry("estado", "Inactive");
        assertThat(info.getDatosAdicionales()).containsEntry("discontinuada", "Sí");
    }

    @Test
    void discontinuacionNoSobrescribeElEstadoActivoInactivo() throws Exception {
        ScopusFuenteRepository repository = mock(ScopusFuenteRepository.class);

        provider(repository).importar(excelDiscontinuado());

        ArgumentCaptor<ScopusFuente> captor = ArgumentCaptor.forClass(ScopusFuente.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("Inactive");
        assertThat(captor.getValue().isDiscontinuada()).isTrue();
    }

    @Test
    void cuentaTodaLaHojaPrincipalYOmiteHojasSecundarias() throws Exception {
        ScopusFuenteRepository repository = mock(ScopusFuenteRepository.class);

        var result = provider(repository).importar(excelConConteos());

        assertThat(result.getRegistrosLeidos()).isEqualTo(3);
        assertThat(result.getRegistrosInsertados()).isEqualTo(2);
        assertThat(result.getRegistrosActualizados()).isZero();
        assertThat(result.getRegistrosRechazados()).isEqualTo(1);
        verify(repository).deleteAllInBatch();
        verify(repository, times(2)).save(any(ScopusFuente.class));
        verify(repository).flush();
    }

    private ScopusCatalogProvider provider(ScopusFuenteRepository repository) {
        return new ScopusCatalogProvider(repository, mock(AuditoriaService.class),
                "https://www.elsevier.com/en-gb/products/scopus/content",
                1000, 1000, "target/scopus-test.sha256", false);
    }

    private MockMultipartFile excel() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Scopus Sources");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Print-ISSN");
            header.createCell(1).setCellValue("E-ISSN");
            header.createCell(2).setCellValue("Active or Inactive");
            header.createCell(3).setCellValue("Publication frequency");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("1234-5678");
            row.createCell(1).setCellValue("2049-3630");
            row.createCell(2).setCellValue("Active");
            row.createCell(3).setCellValue("Monthly");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        return new MockMultipartFile("archivo", "scopus.xlsx", XLSX, bytes);
    }

    private MockMultipartFile excelDiscontinuado() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Scopus Sources Jun. 2026");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("ISSN");
            header.createCell(1).setCellValue("EISSN");
            header.createCell(2).setCellValue("Active or Inactive");
            header.createCell(3).setCellValue("Titles Discontinued by Scopus");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("1234-5678");
            row.createCell(1).setCellValue("2049-3630");
            row.createCell(2).setCellValue("Inactive");
            row.createCell(3).setCellValue("Discontinuation");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        return new MockMultipartFile("archivo", "scopus.xlsx", XLSX, bytes);
    }

    private MockMultipartFile excelConConteos() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            var principal = workbook.createSheet("Scopus Sources Jun. 2026");
            var header = principal.createRow(0);
            header.createCell(0).setCellValue("ISSN");
            header.createCell(1).setCellValue("EISSN");
            header.createCell(2).setCellValue("Active or Inactive");
            principal.createRow(1).createCell(0).setCellValue("1234-5678");
            principal.createRow(2).createCell(1).setCellValue("2049-3630");
            principal.createRow(3).createCell(0).setCellValue("identificador inválido");

            var secundaria = workbook.createSheet("Accepted Titles Jun. 2026");
            var secundariaHeader = secundaria.createRow(0);
            secundariaHeader.createCell(0).setCellValue("ISSN");
            secundaria.createRow(1).createCell(0).setCellValue("1111-2222");

            workbook.write(output);
            bytes = output.toByteArray();
        }
        return new MockMultipartFile("archivo", "scopus.xlsx", XLSX, bytes);
    }

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
}
