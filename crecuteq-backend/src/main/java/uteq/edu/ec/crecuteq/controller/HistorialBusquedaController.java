package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.crecuteq.entity.HistorialBusqueda;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.service.HistorialBusquedaService;
import uteq.edu.ec.crecuteq.security.CurrentUserAccess;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
public class HistorialBusquedaController {

    private final HistorialBusquedaService historialService;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserAccess currentUserAccess;

    public HistorialBusquedaController(
            HistorialBusquedaService historialService,
            UsuarioRepository usuarioRepository,
            CurrentUserAccess currentUserAccess
    ) {
        this.historialService = historialService;
        this.usuarioRepository = usuarioRepository;
        this.currentUserAccess = currentUserAccess;
    }

    @GetMapping("/{usuarioId}")
    public List<HistorialBusqueda> listar(@PathVariable Long usuarioId, Authentication authentication) {

        currentUserAccess.requireSelfOrAdmin(authentication, usuarioId);

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();

        return historialService.listarPorUsuario(usuario);

    }
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id, Authentication authentication) {

        historialService.eliminar(id, currentUserAccess.requireUser(authentication),
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));

    }
}
