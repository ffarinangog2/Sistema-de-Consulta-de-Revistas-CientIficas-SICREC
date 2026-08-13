package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.crecuteq.entity.Favorito;
import uteq.edu.ec.crecuteq.security.CurrentUserAccess;
import uteq.edu.ec.crecuteq.service.FavoritoService;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final CurrentUserAccess currentUserAccess;

    public FavoritoController(FavoritoService favoritoService, CurrentUserAccess currentUserAccess) {
        this.favoritoService = favoritoService;
        this.currentUserAccess = currentUserAccess;
    }

    @PostMapping
    public Favorito guardar(@RequestBody Favorito favorito, Authentication authentication) {

        favorito.setUsuarioId(currentUserAccess.requireUser(authentication).getId());

        return favoritoService.guardar(favorito);

    }

    @GetMapping("/{usuarioId}")
    public List<Favorito> listar(
            @PathVariable Long usuarioId,
            Authentication authentication
    ) {

        currentUserAccess.requireSelfOrAdmin(authentication, usuarioId);

        return favoritoService.listar(usuarioId);

    }

    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Long id,
            Authentication authentication
    ) {

        favoritoService.eliminar(id, currentUserAccess.requireUser(authentication),
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));

    }

}
