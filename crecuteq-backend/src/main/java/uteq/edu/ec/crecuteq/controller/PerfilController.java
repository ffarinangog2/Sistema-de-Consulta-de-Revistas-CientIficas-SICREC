package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.crecuteq.security.CurrentUserAccess;
import uteq.edu.ec.crecuteq.dto.PerfilDTO;
import uteq.edu.ec.crecuteq.service.PerfilService;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;
    private final CurrentUserAccess currentUserAccess;

    public PerfilController(
            PerfilService perfilService,
            CurrentUserAccess currentUserAccess
    ) {

        this.perfilService = perfilService;
        this.currentUserAccess = currentUserAccess;

    }

    @GetMapping("/{idUsuario}")
    public PerfilDTO obtenerPerfil(
            @PathVariable Long idUsuario,
            Authentication authentication
    ) {

        currentUserAccess.requireSelfOrAdmin(authentication, idUsuario);

        return perfilService.obtenerPerfil(idUsuario);

    }

}
