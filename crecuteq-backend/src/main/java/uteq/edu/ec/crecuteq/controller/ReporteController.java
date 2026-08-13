package uteq.edu.ec.crecuteq.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.crecuteq.dto.ReporteHistorialDTO;
import uteq.edu.ec.crecuteq.dto.ReporteAnaliticaDTO;
import uteq.edu.ec.crecuteq.service.ReporteService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(
            ReporteService reporteService
    ) {

        this.reporteService = reporteService;

    }

    @GetMapping("/historial")
    public List<ReporteHistorialDTO> obtenerReporteHistorial() {

        return reporteService.obtenerReporteHistorial();

    }

    @GetMapping("/historial/fechas")
    public List<ReporteHistorialDTO> obtenerReporteHistorialPorFechas(

            @RequestParam String fechaInicio,

            @RequestParam String fechaFin

    ) {

        return reporteService.obtenerReporteHistorialPorFechas(

                fechaInicio,

                fechaFin

        );

    }

    @GetMapping("/historial/analitica")
    public ReporteAnaliticaDTO obtenerAnalitica(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "0") int pagina
    ) {
        return reporteService.obtenerAnalitica(fechaInicio, fechaFin, pagina, 10);
    }

    @GetMapping("/historial/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin
    ) throws IOException {

        byte[] archivo = reporteService.exportarHistorialExcel(fechaInicio, fechaFin);

        return ResponseEntity.ok()

                .header(

                        HttpHeaders.CONTENT_DISPOSITION,

                        "attachment; filename=Reporte_Historial.xlsx"

                )

                .contentType(

                        MediaType.APPLICATION_OCTET_STREAM

                )

                .body(archivo);

    }
    @GetMapping("/historial/pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin
    ) throws Exception {

        byte[] archivo = reporteService.exportarHistorialPDF(fechaInicio, fechaFin);

        return ResponseEntity.ok()

                .header(

                        HttpHeaders.CONTENT_DISPOSITION,

                        "attachment; filename=Reporte_Historial.pdf"

                )

                .contentType(

                        MediaType.APPLICATION_PDF

                )

                .body(archivo);

    }
}
