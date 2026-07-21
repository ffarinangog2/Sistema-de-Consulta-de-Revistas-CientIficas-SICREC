package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.ReporteHistorialDTO;
import uteq.edu.ec.sicrec.projection.ReporteHistorialProjection;
import uteq.edu.ec.sicrec.repository.HistorialBusquedaRepository;

import java.io.IOException;
import java.util.List;

@Service
public class ReporteService {

    private final HistorialBusquedaRepository historialRepository;
    private final ExcelService excelService;
    private final PdfService pdfService;
    private final AuditoriaService auditoriaService;

    public ReporteService(
            HistorialBusquedaRepository historialRepository,
            ExcelService excelService,
            PdfService pdfService,
            AuditoriaService auditoriaService
    ) {

        this.historialRepository = historialRepository;
        this.excelService = excelService;
        this.pdfService = pdfService;
        this.auditoriaService = auditoriaService;

    }

    public List<ReporteHistorialDTO> obtenerReporteHistorial() {

        return historialRepository
                .obtenerReporteHistorial()
                .stream()
                .map(this::convertirDTO)
                .toList();

    }

    private ReporteHistorialDTO convertirDTO(
            ReporteHistorialProjection projection
    ) {

        return new ReporteHistorialDTO(

                projection.getFecha(),

                projection.getUsuario(),

                projection.getTermino(),

                projection.getResultados()

        );

    }
    public List<ReporteHistorialDTO> obtenerReporteHistorialPorFechas(
            String fechaInicio,
            String fechaFin
    ) {

        return historialRepository
                .obtenerReporteHistorialPorFechas(
                        fechaInicio,
                        fechaFin
                )
                .stream()
                .map(this::convertirDTO)
                .toList();

    }

    // INICIO - Auditoría módulo Reportes
    public byte[] exportarHistorialExcel() throws IOException {

        try {
            byte[] archivo = excelService.generarReporteHistorialExcel(
                    obtenerReporteHistorial()
            );

            auditoriaService.registrarExito(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_EXCEL",
                    "Reporte de historial exportado a Excel"
            );

            return archivo;
        } catch (IOException | RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_EXCEL",
                    "No fue posible exportar el reporte de historial a Excel"
            );
            throw e;
        }
    }

    public byte[] exportarHistorialPDF() throws Exception {

        try {
            byte[] archivo = pdfService.generarReporteHistorialPDF(
                    obtenerReporteHistorial()
            );

            auditoriaService.registrarExito(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_PDF",
                    "Reporte de historial exportado a PDF"
            );

            return archivo;
        } catch (Exception e) {
            auditoriaService.registrarError(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_PDF",
                    "No fue posible exportar el reporte de historial a PDF"
            );
            throw e;
        }
    }
    // FIN - Auditoría módulo Reportes

}
