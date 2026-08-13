package uteq.edu.ec.crecuteq.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

@Component
public class CurrentUserAccess {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserAccess(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }

        return usuarioRepository.findByUsuario(authentication.getName())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La sesión ya no corresponde a un usuario activo"));
    }

    public Usuario requireSelfOrAdmin(Authentication authentication, Long requestedUserId) {
        Usuario currentUser = requireUser(authentication);
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!admin && !currentUser.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede acceder a información de otro usuario");
        }
        return currentUser;
    }
}
