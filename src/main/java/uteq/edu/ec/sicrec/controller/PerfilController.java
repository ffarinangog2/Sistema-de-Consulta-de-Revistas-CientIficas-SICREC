package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.PerfilDTO;
import uteq.edu.ec.sicrec.service.PerfilService;

@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "*")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(
            PerfilService perfilService
    ) {

        this.perfilService = perfilService;

    }

    @GetMapping("/{idUsuario}")
    public PerfilDTO obtenerPerfil(
            @PathVariable Long idUsuario
    ) {

        return perfilService.obtenerPerfil(idUsuario);

    }

}