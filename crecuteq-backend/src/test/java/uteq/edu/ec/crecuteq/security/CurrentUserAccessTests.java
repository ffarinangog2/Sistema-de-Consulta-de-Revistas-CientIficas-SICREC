package uteq.edu.ec.crecuteq.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserAccessTests {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final CurrentUserAccess access = new CurrentUserAccess(repository);

    @Test
    void usuarioPuedeAccederASuPropioRecurso() {
        Usuario usuario = usuario(7L, "usuario7");
        var authentication = autenticacion("usuario7", "ROLE_USUARIO");
        when(repository.findByUsuario("usuario7")).thenReturn(Optional.of(usuario));

        assertSame(usuario, access.requireSelfOrAdmin(authentication, 7L));
    }

    @Test
    void usuarioNoPuedeAccederARecursoAjeno() {
        Usuario usuario = usuario(7L, "usuario7");
        var authentication = autenticacion("usuario7", "ROLE_USUARIO");
        when(repository.findByUsuario("usuario7")).thenReturn(Optional.of(usuario));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> access.requireSelfOrAdmin(authentication, 8L));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void administradorPuedeAccederARecursoAjeno() {
        Usuario admin = usuario(1L, "admin");
        var authentication = autenticacion("admin", "ROLE_ADMIN");
        when(repository.findByUsuario("admin")).thenReturn(Optional.of(admin));

        assertSame(admin, access.requireSelfOrAdmin(authentication, 8L));
    }

    @Test
    void sesionSinUsuarioActivoEsRechazada() {
        var authentication = autenticacion("eliminado", "ROLE_USUARIO");
        when(repository.findByUsuario("eliminado")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> access.requireUser(authentication));

        assertEquals(401, error.getStatusCode().value());
    }

    private UsernamePasswordAuthenticationToken autenticacion(String nombre, String rol) {
        return new UsernamePasswordAuthenticationToken(
                nombre, null, List.of(new SimpleGrantedAuthority(rol)));
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        try {
            var campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        usuario.setUsuario(nombre);
        usuario.setEstado(true);
        return usuario;
    }
}
