package uteq.edu.ec.crecuteq.provider;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.entity.SageRevista;
import uteq.edu.ec.crecuteq.repository.SageRevistaRepository;
import uteq.edu.ec.crecuteq.service.AuditoriaService;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SageProviderTests {
    @TempDir Path temporal;

    @Test void importaGoldConPrecioActualYMoneda() throws Exception {
        SageRevistaRepository repository = vacio();
        assertEquals(1, provider(repository).importar(gold()).getRegistrosInsertados());
        verify(repository).save(argThat(r -> SageProvider.GOLD.equals(r.getModeloPublicacion())
                && "2100".equals(r.getApcUsd()) && "AAT".equals(r.getTla())));
    }

    @Test void importaSageChoiceConIssnYAmbasMonedas() throws Exception {
        SageRevistaRepository repository = vacio();
        assertEquals(1, provider(repository).importar(choice()).getRegistrosInsertados());
        verify(repository).save(argThat(r -> SageProvider.CHOICE.equals(r.getModeloPublicacion())
                && "23297662".equals(r.getIssnNormalizado()) && "4440".equals(r.getApcUsd())
                && "3255.852".equals(r.getApcGbp())));
    }

    @Test void buscaChoicePorEissnYExponeApc() {
        SageRevista r = new SageRevista(); r.setTitulo("3D Printing and Additive Manufacturing");
        r.setJournalCode("J949"); r.setModeloPublicacion(SageProvider.CHOICE); r.setIssn("2329-7662");
        r.setEissn("2329-7670"); r.setApcUsd("4440"); r.setApcGbp("3255.852");
        SageRevistaRepository repository = mock(SageRevistaRepository.class);
        when(repository.findFirstByIssnNormalizadoOrEissnNormalizado(null, "23297670")).thenReturn(Optional.of(r));
        var info = provider(repository).buscar(null, null, "2329-7670").orElseThrow();
        assertEquals("sage", info.getProveedor()); assertEquals("4440", info.getApc());
        assertEquals("3255.852", info.getDatosAdicionales().get("apcGbp"));
    }

    private SageProvider provider(SageRevistaRepository repository) {
        return new SageProvider(repository, mock(AuditoriaService.class), HttpClient.newHttpClient(),
                URI.create("https://sage.invalid/source"), URI.create("https://sage.invalid/gold.xlsx"),
                URI.create("https://sage.invalid/choice.xlsx"), Duration.ofSeconds(1),
                temporal.resolve("gold.sha256"), temporal.resolve("choice.sha256"), false);
    }
    private SageRevistaRepository vacio() {
        SageRevistaRepository r = mock(SageRevistaRepository.class);
        when(r.findFirstByModeloPublicacionAndJournalCodeIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        return r;
    }
    private MockMultipartFile gold() throws Exception {
        return excel(new String[]{"Journal Title1","SJ Site","Journal Code","TLA","Journal","List Price 2026","Current Price 2026","Currency"},
                new Object[]{"AATCC Journal of Research","https://journals.sagepub.com/home/AAT","L620","AAT","AATCC Journal of Research",2100,2100,"USD"}, "sage-gold.xlsx");
    }
    private MockMultipartFile choice() throws Exception {
        return excel(new String[]{"Journal Title","Journal Code","ISSN","EISSN","Division","2026 OA APC ($)","2026 OA APC (£)","Journal URL"},
                new Object[]{"3D Printing and Additive Manufacturing","J949","2329-7662","2329-7670","STM",4440,3255.852,"https://journals.sagepub.com/home/TDP"}, "sage-choice.xlsx");
    }
    private MockMultipartFile excel(String[] headers, Object[] values, String name) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("APCs"); var h = sheet.createRow(0); var row = sheet.createRow(1);
            for (int i=0;i<headers.length;i++) { h.createCell(i).setCellValue(headers[i]);
                if(values[i] instanceof Number n) row.createCell(i).setCellValue(n.doubleValue());
                else row.createCell(i).setCellValue(values[i].toString()); }
            workbook.write(output); return new MockMultipartFile("archivo",name,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",output.toByteArray());
        }
    }
}
