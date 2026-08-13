package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import uteq.edu.ec.crecuteq.dto.LoginRequestDTO;
import uteq.edu.ec.crecuteq.dto.LoginResponseDTO;
import uteq.edu.ec.crecuteq.entity.Rol;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.*;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UsuarioLoginRolTests {

    @Test
    void loginDevuelveRolUsuarioParaLaSesionDelFrontend() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        UsuarioService service = new UsuarioService(
                usuarios, encoder, jwt, mock(RolRepository.class), mock(CargoRepository.class),
                mock(EmailService.class), mock(TokenRecuperacionRepository.class),
                mock(AuditoriaService.class), mock(PerfilAcademicoService.class),
                mock(PerfilAcademicoRepository.class));

        Rol rol = new Rol();
        rol.setNombreRol("USUARIO");
        Usuario usuario = new Usuario();
        usuario.setUsuario("usuario1");
        usuario.setNombreCompleto("Usuario Uno");
        usuario.setCorreoInstitucional("usuario1@uteq.edu.ec");
        usuario.setPassword("hash");
        usuario.setEstado(true);
        usuario.setRol(rol);
        usuario.setDebeCambiarPassword(false);
        usuario.setCuentaBloqueada(false);

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsuario("usuario1");
        request.setPassword("Clave123");
        when(usuarios.findByUsuario("usuario1")).thenReturn(Optional.of(usuario));
        when(encoder.matches("Clave123", "hash")).thenReturn(true);
        when(jwt.generarToken("usuario1")).thenReturn("token");

        LoginResponseDTO respuesta = service.login(request);

        assertEquals("USUARIO", respuesta.getRol());
        assertEquals("token", respuesta.getToken());
    }
}
