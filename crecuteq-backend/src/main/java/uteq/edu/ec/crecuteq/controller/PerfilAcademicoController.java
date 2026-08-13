package uteq.edu.ec.crecuteq.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoRequestDTO;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoResponseDTO;
import uteq.edu.ec.crecuteq.service.PerfilAcademicoService;


// INICIO - Perfil Académico
@RestController
@RequestMapping("/api/perfil-academico")
public class PerfilAcademicoController {

    private final PerfilAcademicoService perfilAcademicoService;

    public PerfilAcademicoController(PerfilAcademicoService perfilAcademicoService) {
        this.perfilAcademicoService = perfilAcademicoService;
    }

    @GetMapping
    public PerfilAcademicoResponseDTO obtener(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return perfilAcademicoService.obtener(authorization);
    }

    @PostMapping
    public ResponseEntity<PerfilAcademicoResponseDTO> crear(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PerfilAcademicoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilAcademicoService.crear(authorization, request));
    }

    @PutMapping
    public PerfilAcademicoResponseDTO actualizar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PerfilAcademicoRequestDTO request) {
        return perfilAcademicoService.actualizar(authorization, request);
    }

}
// FIN - Perfil Académico
