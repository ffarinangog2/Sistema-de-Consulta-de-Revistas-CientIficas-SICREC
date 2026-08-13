package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CambiarPasswordDTO;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.*;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UsuarioCambiarPasswordTests {

    private UsuarioRepository repository;
    private BCryptPasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(BCryptPasswordEncoder.class);
        service = new UsuarioService(repository, encoder, mock(JwtService.class),
                mock(RolRepository.class), mock(CargoRepository.class), mock(EmailService.class),
                mock(TokenRecuperacionRepository.class), mock(AuditoriaService.class),
                mock(PerfilAcademicoService.class), mock(PerfilAcademicoRepository.class));
    }

    @Test
    void cambiaSoloLaCuentaIndicadaPorLaSesion() {
        Usuario autenticado = new Usuario();
        autenticado.setPassword("hash-actual");
        CambiarPasswordDTO dto = datosValidos();
        dto.setIdUsuario(999L);
        when(repository.findById(7L)).thenReturn(Optional.of(autenticado));
        when(encoder.matches("Actual123", "hash-actual")).thenReturn(true);
        when(encoder.encode("Nueva123")).thenReturn("hash-nuevo");

        service.cambiarPassword(7L, dto);

        verify(repository).findById(7L);
        verify(repository, never()).findById(999L);
        verify(repository).save(autenticado);
        assertEquals("hash-nuevo", autenticado.getPassword());
    }

    @Test
    void passwordActualIncorrectaDevuelve401() {
        Usuario autenticado = new Usuario();
        autenticado.setPassword("hash-actual");
        when(repository.findById(7L)).thenReturn(Optional.of(autenticado));
        when(encoder.matches(anyString(), eq("hash-actual"))).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cambiarPassword(7L, datosValidos()));

        assertEquals(401, error.getStatusCode().value());
        assertEquals("La contraseña actual es incorrecta", error.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void passwordsNuevasDistintasDevuelven400() {
        Usuario autenticado = new Usuario();
        autenticado.setPassword("hash-actual");
        CambiarPasswordDTO dto = datosValidos();
        dto.setConfirmarPassword("Distinta123");
        when(repository.findById(7L)).thenReturn(Optional.of(autenticado));
        when(encoder.matches("Actual123", "hash-actual")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cambiarPassword(7L, dto));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("Las contraseñas no coinciden", error.getReason());
        verify(repository, never()).save(any());
    }

    private CambiarPasswordDTO datosValidos() {
        CambiarPasswordDTO dto = new CambiarPasswordDTO();
        dto.setPasswordActual("Actual123");
        dto.setNuevaPassword("Nueva123");
        dto.setConfirmarPassword("Nueva123");
        return dto;
    }
}
