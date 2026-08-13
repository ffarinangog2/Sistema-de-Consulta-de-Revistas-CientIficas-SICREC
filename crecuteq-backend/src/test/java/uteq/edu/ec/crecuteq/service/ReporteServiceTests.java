package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.crecuteq.projection.ReporteHistorialProjection;
import uteq.edu.ec.crecuteq.repository.HistorialBusquedaRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteServiceTests {
    private HistorialBusquedaRepository repository;
    private ExcelService excelService;
    private PdfService pdfService;
    private ReporteService service;

    @BeforeEach
    void configurar() {
        repository = mock(HistorialBusquedaRepository.class);
        excelService = mock(ExcelService.class);
        pdfService = mock(PdfService.class);
        service = new ReporteService(repository, excelService, pdfService,
                mock(AuditoriaService.class));
    }

    @Test
    void excelUsaElMismoRangoDeFechasDelReporte() throws Exception {
        ReporteHistorialProjection projection = proyeccion("software", 4);
        when(repository.obtenerReporteHistorialPorFechas("2026-07-01", "2026-07-31"))
                .thenReturn(List.of(projection));
        when(excelService.generarReporteHistorialExcel(anyList()))
                .thenReturn(new byte[]{1, 2});

        byte[] archivo = service.exportarHistorialExcel("2026-07-01", "2026-07-31");

        assertThat(archivo).containsExactly(1, 2);
        verify(repository).obtenerReporteHistorialPorFechas("2026-07-01", "2026-07-31");
        verify(excelService).generarReporteHistorialExcel(anyList());
    }

    @Test
    void pdfUsaElMismoRangoDeFechasDelReporte() throws Exception {
        ReporteHistorialProjection projection = proyeccion("veterinaria", 0);
        when(repository.obtenerReporteHistorialPorFechas("2026-08-01", "2026-08-03"))
                .thenReturn(List.of(projection));
        when(pdfService.generarReporteHistorialPDF(anyList()))
                .thenReturn(new byte[]{3, 4});

        byte[] archivo = service.exportarHistorialPDF("2026-08-01", "2026-08-03");

        assertThat(archivo).containsExactly(3, 4);
        verify(repository).obtenerReporteHistorialPorFechas("2026-08-01", "2026-08-03");
        verify(pdfService).generarReporteHistorialPDF(anyList());
    }

    private ReporteHistorialProjection proyeccion(String termino, int resultados) {
        return new ReporteHistorialProjection() {
            @Override public LocalDateTime getFecha() {
                return LocalDateTime.of(2026, 8, 3, 10, 0);
            }
            @Override public String getUsuario() { return "Usuario real"; }
            @Override public String getTermino() { return termino; }
            @Override public Integer getResultados() { return resultados; }
        };
    }
}
