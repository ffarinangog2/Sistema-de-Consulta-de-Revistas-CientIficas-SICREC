package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.dto.ReporteAnaliticaDTO;
import uteq.edu.ec.crecuteq.dto.ReporteHistorialDTO;
import uteq.edu.ec.crecuteq.projection.ReporteHistorialProjection;
import uteq.edu.ec.crecuteq.projection.ReporteResumenProjection;
import uteq.edu.ec.crecuteq.projection.ReporteSerieProjection;
import uteq.edu.ec.crecuteq.repository.HistorialBusquedaRepository;

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

    public ReporteAnaliticaDTO obtenerAnalitica(
            String fechaInicio, String fechaFin, int pagina, int tamano) {
        String inicio = normalizarFiltro(fechaInicio);
        String fin = normalizarFiltro(fechaFin);
        int paginaSolicitada = Math.max(0, pagina);
        int tamanoPagina = Math.max(1, tamano);

        ReporteResumenProjection resumen = historialRepository.obtenerResumenReporte(inicio, fin);
        long total = valor(resumen.getTotal());
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanoPagina);
        int paginaValida = totalPaginas == 0 ? 0 : Math.min(paginaSolicitada, totalPaginas - 1);

        List<ReporteHistorialDTO> contenido = historialRepository.obtenerPaginaReporte(
                        inicio, fin, tamanoPagina, paginaValida * tamanoPagina)
                .stream().map(this::convertirDTO).toList();

        return new ReporteAnaliticaDTO(
                new ReporteAnaliticaDTO.Resumen(total, valor(resumen.getConResultados()),
                        valor(resumen.getSinResultados()), valor(resumen.getUsuarios())),
                convertirSerie(historialRepository.obtenerBusquedasPorDia(inicio, fin)),
                convertirSerie(historialRepository.obtenerTerminosReporte(inicio, fin)),
                convertirSerie(historialRepository.obtenerUsuariosReporte(inicio, fin)),
                new ReporteAnaliticaDTO.Pagina(contenido, paginaValida, tamanoPagina,
                        total, totalPaginas)
        );
    }

    private List<ReporteAnaliticaDTO.Serie> convertirSerie(List<ReporteSerieProjection> serie) {
        return serie.stream()
                .map(item -> new ReporteAnaliticaDTO.Serie(item.getEtiqueta(), valor(item.getTotal())))
                .toList();
    }

    private long valor(Long valor) {
        return valor == null ? 0 : valor;
    }

    private String normalizarFiltro(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    // INICIO - Auditoría módulo Reportes
    public byte[] exportarHistorialExcel() throws IOException {
        return exportarHistorialExcel(null, null);
    }

    public byte[] exportarHistorialExcel(String fechaInicio, String fechaFin) throws IOException {

        try {
            byte[] archivo = excelService.generarReporteHistorialExcel(
                    obtenerDatosExportacion(fechaInicio, fechaFin)
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
        return exportarHistorialPDF(null, null);
    }

    public byte[] exportarHistorialPDF(String fechaInicio, String fechaFin) throws Exception {

        try {
            byte[] archivo = pdfService.generarReporteHistorialPDF(
                    obtenerDatosExportacion(fechaInicio, fechaFin)
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

    private List<ReporteHistorialDTO> obtenerDatosExportacion(
            String fechaInicio, String fechaFin) {
        return fechaInicio != null && !fechaInicio.isBlank()
                && fechaFin != null && !fechaFin.isBlank()
                ? obtenerReporteHistorialPorFechas(fechaInicio, fechaFin)
                : obtenerReporteHistorial();
    }
    // FIN - Auditoría módulo Reportes

}
