package uteq.edu.ec.sicrec.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.sicrec.dto.AuditoriaResponseDTO;
import uteq.edu.ec.sicrec.service.AuditoriaService;
import uteq.edu.ec.sicrec.service.ExcelService;
import uteq.edu.ec.sicrec.service.PdfService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

// INICIO - Endpoints de auditoría
@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final ExcelService excelService;
    private final PdfService pdfService;

    public AuditoriaController(
            AuditoriaService auditoriaService,
            ExcelService excelService,
            PdfService pdfService
    ) {

        this.auditoriaService = auditoriaService;
        this.excelService = excelService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public ResponseEntity<?> listarAuditoria(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaHasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Integer page,
            @PageableDefault(
                    sort = "fechaAccion",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        // INICIO - Filtros de auditoría
        if (page != null) {
            return ResponseEntity.ok(
                    auditoriaService.listarAuditoria(
                            usuario,
                            modulo,
                            accion,
                            resultado,
                            fechaDesde,
                            fechaHasta,
                            busqueda,
                            pageable
                    )
            );
        }

        return ResponseEntity.ok(
                obtenerRegistros(
                        usuario,
                        modulo,
                        accion,
                        resultado,
                        fechaDesde,
                        fechaHasta,
                        busqueda
                )
        );
        // FIN - Filtros de auditoría
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaHasta,
            @RequestParam(required = false) String busqueda
    ) throws IOException {

        // INICIO - Auditoría módulo Reportes
        byte[] archivo;

        try {
            archivo = excelService.generarAuditoriaExcel(
                    obtenerRegistros(
                            usuario,
                            modulo,
                            accion,
                            resultado,
                            fechaDesde,
                            fechaHasta,
                            busqueda
                    )
            );

            auditoriaService.registrarExito(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_EXCEL",
                    "Auditoría general exportada a Excel"
            );
        } catch (IOException | RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_EXCEL",
                    "No fue posible exportar la auditoría a Excel"
            );
            throw e;
        }
        // FIN - Auditoría módulo Reportes

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Auditoria_SICREC.xlsx"
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(archivo);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaHasta,
            @RequestParam(required = false) String busqueda
    ) throws Exception {

        // INICIO - Auditoría módulo Reportes
        byte[] archivo;

        try {
            archivo = pdfService.generarAuditoriaPDF(
                    obtenerRegistros(
                            usuario,
                            modulo,
                            accion,
                            resultado,
                            fechaDesde,
                            fechaHasta,
                            busqueda
                    )
            );

            auditoriaService.registrarExito(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_PDF",
                    "Auditoría general exportada a PDF"
            );
        } catch (Exception e) {
            auditoriaService.registrarError(
                    null,
                    "REPORTES",
                    "EXPORTACIÓN_PDF",
                    "No fue posible exportar la auditoría a PDF"
            );
            throw e;
        }
        // FIN - Auditoría módulo Reportes

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Auditoria_SICREC.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo);
    }

    private List<AuditoriaResponseDTO> obtenerRegistros(
            String usuario,
            String modulo,
            String accion,
            String resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String busqueda
    ) {

        return auditoriaService.listarAuditoria(
                usuario,
                modulo,
                accion,
                resultado,
                fechaDesde,
                fechaHasta,
                busqueda
        );
    }
}
// FIN - Endpoints de auditoría
