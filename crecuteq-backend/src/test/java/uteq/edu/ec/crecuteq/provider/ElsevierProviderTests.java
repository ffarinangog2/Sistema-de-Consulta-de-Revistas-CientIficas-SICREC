package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.ElsevierRevista;
import uteq.edu.ec.crecuteq.repository.ElsevierRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ElsevierProviderTests {
    @TempDir Path temporal;

    @Test
    void importaLasColumnasOficialesDelExcel() throws Exception {
        ElsevierRevistaRepository repository = mock(ElsevierRevistaRepository.class);
        when(repository.findFirstByIssnNormalizado("18762859")).thenReturn(Optional.empty());

        var resultado = provider(repository).importar(excel("1876-2859", "Academic Pediatrics", "Hybrid"));

        assertEquals(1, resultado.getRegistrosLeidos());
        assertEquals(1, resultado.getRegistrosInsertados());
        verify(repository).saveAndFlush(argThat(r ->
                "3720".equals(r.getApcUsd()) && "3390".equals(r.getApcEur())
                        && "09-Apr-2026".equals(r.getVigencia())));
    }

    @Test
    void actualizaPorIssnSinDuplicar() throws Exception {
        ElsevierRevista existente = new ElsevierRevista();
        existente.setIssnNormalizado("18762859");
        ElsevierRevistaRepository repository = mock(ElsevierRevistaRepository.class);
        when(repository.findFirstByIssnNormalizado("18762859")).thenReturn(Optional.of(existente));

        var resultado = provider(repository).importar(excel("1876-2859", "Título actualizado", "Hybrid"));

        assertEquals(0, resultado.getRegistrosInsertados());
        assertEquals(1, resultado.getRegistrosActualizados());
        assertEquals("Título actualizado", existente.getTitulo());
        verify(repository).saveAndFlush(existente);
    }

    @Test
    void buscaElIssnOficialContraElEissnConsultado() {
        ElsevierRevista revista = new ElsevierRevista();
        revista.setIssn("1876-2859");
        revista.setTitulo("Academic Pediatrics");
        revista.setModeloPublicacion("Hybrid");
        revista.setApcUsd("3720");
        revista.setApcJpy("484460");
        ElsevierRevistaRepository repository = mock(ElsevierRevistaRepository.class);
        when(repository.findFirstByIssnNormalizado("18762859")).thenReturn(Optional.of(revista));

        JournalInfo info = provider(repository).buscar(null, null, "1876-2859").orElseThrow();

        assertEquals("elsevier", info.getProveedor());
        assertEquals("3720", info.getApc());
        assertEquals("484460", info.getDatosAdicionales().get("apcJpy"));
        assertEquals("No", info.getDatosAdicionales().get("openAccess"));
    }

    private ElsevierProvider provider(ElsevierRevistaRepository repository) {
        return new ElsevierProvider(repository, mock(AuditoriaService.class), HttpClient.newHttpClient(),
                URI.create("https://elsevier.invalid/apc.xlsx"), Duration.ofSeconds(1),
                temporal.resolve("elsevier.sha256"), false);
    }

    private MockMultipartFile excel(String issn, String titulo, String modelo) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("APC prices");
            sheet.createRow(0).createCell(0).setCellValue("Article Publishing Charge (APC) price list *");
            sheet.createRow(1).createCell(0).setCellValue("All prices excluding taxes. Prices as of date: 09-Apr-2026");
            var header = sheet.createRow(2);
            String[] columnas = {"ISSN", "Title", "Business model", "USD", "EUR", "GBP", "JPY"};
            for (int i = 0; i < columnas.length; i++) header.createCell(i).setCellValue(columnas[i]);
            var row = sheet.createRow(3);
            row.createCell(0).setCellValue(issn); row.createCell(1).setCellValue(titulo);
            row.createCell(2).setCellValue(modelo); row.createCell(3).setCellValue(3720);
            row.createCell(4).setCellValue(3390); row.createCell(5).setCellValue(2980);
            row.createCell(6).setCellValue(484460);
            workbook.write(output);
            return new MockMultipartFile("archivo", "elsevier-apc.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
