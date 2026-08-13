package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.FotoPerfil;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.FotoPerfilRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FotoPerfilServiceTests {
    private FotoPerfilRepository repository;
    private FotoPerfilService service;

    @BeforeEach
    void configurar() {
        repository = mock(FotoPerfilRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        JwtService jwt = mock(JwtService.class);
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 8L);
        when(jwt.obtenerSubject("token-valido")).thenReturn("usuario8");
        when(usuarios.findByUsuario("usuario8")).thenReturn(Optional.of(usuario));
        service = new FotoPerfilService(repository, usuarios, jwt);
    }

    @Test
    void agregaUnaFotoValidaAlUsuarioAutenticado() throws Exception {
        var archivo = new MockMultipartFile("archivo", "perfil.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});

        service.guardar("Bearer token-valido", archivo);

        var captor = org.mockito.ArgumentCaptor.forClass(FotoPerfil.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTipoContenido()).isEqualTo("image/png");
        assertThat(captor.getValue().getContenido()).isEqualTo(archivo.getBytes());
    }

    @Test
    void reemplazaLaFotoExistenteSinCrearOtroRegistro() {
        FotoPerfil existente = new FotoPerfil();
        when(repository.findByUsuarioId(8L)).thenReturn(Optional.of(existente));
        var archivo = new MockMultipartFile("archivo", "perfil.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        service.guardar("Bearer token-valido", archivo);

        verify(repository).save(existente);
        assertThat(existente.getTipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    void rechazaArchivosQueNoSonImagen() {
        var archivo = new MockMultipartFile("archivo", "documento.pdf", "application/pdf",
                new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.guardar("Bearer token-valido", archivo))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JPG, PNG o WebP");
        verify(repository, never()).save(any());
    }

    @Test
    void eliminaLaFotoDelUsuarioAutenticado() {
        service.eliminar("Bearer token-valido");

        verify(repository).deleteByUsuarioId(8L);
    }
}
