package uteq.edu.ec.crecuteq.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.RevistaDTO;
import uteq.edu.ec.crecuteq.dto.RevistaPaginaDTO;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaRequestDTO;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaResponseDTO;
import uteq.edu.ec.crecuteq.dto.SubjectAreaDTO;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.service.HistorialBusquedaService;
import uteq.edu.ec.crecuteq.service.RevistaService;
import uteq.edu.ec.crecuteq.service.SpringerRevistaService;
import uteq.edu.ec.crecuteq.service.ExcelService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RevistaController {

    private final RevistaService revistaService;
    private final HistorialBusquedaService historialBusquedaService;
    private final UsuarioRepository usuarioRepository;
    // INICIO - Endpoints de proveedores centralizados
    private final SpringerRevistaService springerRevistaService;
    private final ExcelService excelService;
    // FIN - Endpoints de proveedores centralizados

    public RevistaController(
            RevistaService revistaService,
            HistorialBusquedaService historialBusquedaService,
            UsuarioRepository usuarioRepository,
            SpringerRevistaService springerRevistaService,
            ExcelService excelService
    ) {
        this.revistaService = revistaService;
        this.historialBusquedaService = historialBusquedaService;
        this.usuarioRepository = usuarioRepository;
        this.springerRevistaService = springerRevistaService;
        this.excelService = excelService;
    }

    @GetMapping("/revistas/buscar")
    public List<RevistaDTO> buscar(

            @RequestParam String termino,
            @RequestParam(defaultValue = "25") Integer cantidad,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) String facultad,
            @RequestParam(required = false) String campoEstudio

    ) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "usuarioId debe ser un identificador válido."
            );
        }

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el usuario indicado."
                ));

        // INICIO - Auditoría módulo Búsquedas
        List<RevistaDTO> resultado;

        try {
            resultado = revistaService.buscarRevistas(
                    termino,
                    cantidad,
                    facultad,
                    campoEstudio
            );
        } catch (RuntimeException e) {
            if (termino != null && !termino.isBlank()) {
                historialBusquedaService.registrarBusquedaFallida(
                        usuario,
                        termino
                );
            }
            throw e;
        }
        // FIN - Auditoría módulo Búsquedas

        if (termino != null && !termino.isBlank()) {
            historialBusquedaService.guardarBusqueda(
                    usuario,
                    termino,
                    resultado.size()
            );
        }

        return resultado;

    }

    @PostMapping("/revistas/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestBody List<RevistaDTO> revistas
    ) throws IOException {
        byte[] archivo = excelService.generarRevistasExcel(revistas);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Revistas_filtradas.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

    @GetMapping("/revistas/buscar-pagina-campo")
    public RevistaPaginaDTO buscarPaginaCampo(
            @RequestParam Long usuarioId,
            @RequestParam String facultad,
            @RequestParam String campoEstudio,
            @RequestParam(defaultValue = "0") Integer start
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "usuarioId debe ser un identificador válido.");
        }
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el usuario indicado."));
        return revistaService.buscarPaginaCampo(facultad, campoEstudio, start);
    }

    @GetMapping("/revistas/buscar-todas-campo")
    public List<RevistaDTO> buscarTodasCampo(
            @RequestParam Long usuarioId,
            @RequestParam String facultad,
            @RequestParam String campoEstudio
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "usuarioId debe ser un identificador válido.");
        }
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el usuario indicado."));
        return revistaService.buscarTodasCampo(facultad, campoEstudio);
    }

    @GetMapping("/revistas/detalle-complementario")
    public RevistaDTO obtenerDetalleComplementario(
            @RequestParam Long usuarioId,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String issn,
            @RequestParam(required = false) String eIssn,
            @RequestParam(required = false) String editorial
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "usuarioId debe ser un identificador válido.");
        }
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el usuario indicado."));
        return revistaService.obtenerDetalleComplementario(
                sourceId, issn, eIssn, editorial);
    }

    @GetMapping("/revistas/facultades")
    public List<Map<String, String>> obtenerFacultades() {
        return revistaService.obtenerFacultades();
    }

    @GetMapping("/revistas/facultades/{facultad}/campos-estudio")
    public List<SubjectAreaDTO> obtenerCamposEstudio(
            @PathVariable String facultad
    ) {
        return revistaService.obtenerCamposEstudioFacultad(facultad);
    }

    // INICIO - Compatibilidad CRUD del catálogo Springer
    @GetMapping("/springer-revistas")
    public List<SpringerRevistaResponseDTO> listarSpringer() {
        return springerRevistaService.listar();
    }

    @GetMapping("/springer-revistas/{id}")
    public SpringerRevistaResponseDTO buscarSpringerPorId(@PathVariable Long id) {
        return springerRevistaService.buscarPorId(id);
    }

    @PostMapping("/springer-revistas")
    public ResponseEntity<SpringerRevistaResponseDTO> crearSpringer(
            @Valid @RequestBody SpringerRevistaRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(springerRevistaService.crear(request));
    }

    @PutMapping("/springer-revistas/{id}")
    public SpringerRevistaResponseDTO actualizarSpringer(
            @PathVariable Long id,
            @Valid @RequestBody SpringerRevistaRequestDTO request
    ) {
        return springerRevistaService.actualizar(id, request);
    }

    @DeleteMapping("/springer-revistas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSpringer(@PathVariable Long id) {
        springerRevistaService.eliminar(id);
    }
    // FIN - Compatibilidad CRUD del catálogo Springer

}
