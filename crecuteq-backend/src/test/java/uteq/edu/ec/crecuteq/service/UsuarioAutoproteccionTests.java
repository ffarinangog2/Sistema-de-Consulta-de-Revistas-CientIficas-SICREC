package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.*;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UsuarioAutoproteccionTests {

    private UsuarioRepository usuarioRepository;
    private JwtService jwtService;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        jwtService = mock(JwtService.class);
        service = new UsuarioService(
                usuarioRepository,
                mock(BCryptPasswordEncoder.class),
                jwtService,
                mock(RolRepository.class),
                mock(CargoRepository.class),
                mock(EmailService.class),
                mock(TokenRecuperacionRepository.class),
                mock(AuditoriaService.class),
                mock(PerfilAcademicoService.class),
                mock(PerfilAcademicoRepository.class)
        );
    }

    @Test
    void administradorNoPuedeDesactivarSuPropiaCuenta() {
        Usuario actual = usuario(1L, "admin");
        autenticar(actual);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminarUsuario(1L, "Bearer token-admin"));

        assertEquals(403, error.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void administradorNoPuedeEditarNiQuitarSuPropioRol() {
        Usuario actual = usuario(1L, "admin");
        autenticar(actual);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizarUsuario(1L, new Usuario(), "Bearer token-admin"));

        assertEquals(403, error.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void administradorPuedeDesactivarOtraCuenta() {
        Usuario actual = usuario(1L, "admin");
        Usuario otraCuenta = usuario(2L, "usuario2");
        otraCuenta.setEstado(true);
        autenticar(actual);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otraCuenta));

        service.eliminarUsuario(2L, "Bearer token-admin");

        assertEquals(false, otraCuenta.getEstado());
        verify(usuarioRepository).save(otraCuenta);
    }

    private void autenticar(Usuario usuario) {
        when(jwtService.obtenerSubject("token-admin")).thenReturn(usuario.getUsuario());
        when(usuarioRepository.findByUsuario(usuario.getUsuario())).thenReturn(Optional.of(usuario));
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        try {
            var campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        usuario.setUsuario(nombreUsuario);
        return usuario;
    }
}
