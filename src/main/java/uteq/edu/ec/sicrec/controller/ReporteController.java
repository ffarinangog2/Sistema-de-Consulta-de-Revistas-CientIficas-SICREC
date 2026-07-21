package uteq.edu.ec.sicrec.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.ReporteHistorialDTO;
import uteq.edu.ec.sicrec.service.ReporteService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
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

    @GetMapping("/historial/excel")
    public ResponseEntity<byte[]> exportarExcel() throws IOException {

        byte[] archivo = reporteService.exportarHistorialExcel();

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
    public ResponseEntity<byte[]> exportarPDF() throws Exception {

        byte[] archivo = reporteService.exportarHistorialPDF();

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
