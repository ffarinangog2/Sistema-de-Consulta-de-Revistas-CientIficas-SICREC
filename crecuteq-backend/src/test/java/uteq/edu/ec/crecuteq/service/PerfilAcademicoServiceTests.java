package uteq.edu.ec.crecuteq.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoRequestDTO;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoResponseDTO;
import uteq.edu.ec.crecuteq.dto.RegistroUsuarioDTO;
import uteq.edu.ec.crecuteq.entity.PerfilAcademico;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.PerfilAcademicoRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// INICIO - Perfil Académico
class PerfilAcademicoServiceTests {

    private PerfilAcademicoRepository perfilAcademicoRepository;
    private PerfilAcademicoService service;
    private Usuario usuario;

    @BeforeEach
    void configurar() {
        perfilAcademicoRepository = mock(PerfilAcademicoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        JwtService jwtService = mock(JwtService.class);

        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        usuario.setUsuario("investigador");

        when(jwtService.obtenerSubject("token-valido")).thenReturn("investigador");
        when(usuarioRepository.findByUsuario("investigador")).thenReturn(Optional.of(usuario));
        when(perfilAcademicoRepository.save(any(PerfilAcademico.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service = new PerfilAcademicoService(
                perfilAcademicoRepository,
                usuarioRepository,
                jwtService
        );
    }

    @Test
    void devuelveIndicadoresFalsosCuandoElPerfilTodaviaNoExiste() {
        when(perfilAcademicoRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

        PerfilAcademicoResponseDTO response = service.obtener("Bearer token-valido");

        assertThat(response.getUsuarioId()).isEqualTo(7L);
        assertThat(response.getId()).isNull();
        assertThat(response.isTieneGoogleScholar()).isFalse();
        assertThat(response.isTieneOrcid()).isFalse();
    }

    @Test
    void creaUnSoloPerfilConEnlacesValidos() {
        when(perfilAcademicoRepository.existsByUsuarioId(7L)).thenReturn(false);
        PerfilAcademicoRequestDTO request = requestValido();

        PerfilAcademicoResponseDTO response = service.crear("Bearer token-valido", request);

        assertThat(response.getGoogleScholar()).startsWith("https://scholar.google.com/");
        assertThat(response.getOrcid()).isEqualTo("https://orcid.org/0000-0002-1825-0097");
        assertThat(response.isTieneGoogleScholar()).isTrue();
        assertThat(response.isTieneOrcid()).isTrue();
    }

    @Test
    void rechazaCrearUnSegundoPerfilParaElMismoUsuario() {
        when(perfilAcademicoRepository.existsByUsuarioId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.crear("Bearer token-valido", requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void rechazaOrcidConDigitoDeVerificacionIncorrecto() {
        when(perfilAcademicoRepository.existsByUsuarioId(7L)).thenReturn(false);
        PerfilAcademicoRequestDTO request = requestValido();
        request.setOrcid("https://orcid.org/0000-0002-1825-0098");

        assertThatThrownBy(() -> service.crear("Bearer token-valido", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("dígito de verificación");
    }

    @Test
    void validaLosDominiosPermitidosEnElDto() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            PerfilAcademicoRequestDTO request = new PerfilAcademicoRequestDTO();
            request.setGoogleScholar("https://ejemplo.com/perfil");
            request.setOrcid("texto cualquiera");

            assertThat(validator.validate(request)).hasSize(2);
        }
    }

    @Test
    void permiteCrearUsuarioSinEnlacesAcademicos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            RegistroUsuarioDTO request = new RegistroUsuarioDTO();
            completarRegistroValido(request);

            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }

    @Test
    void validaEnlacesAcademicosOpcionalesAlCrearUsuario() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            RegistroUsuarioDTO request = new RegistroUsuarioDTO();
            completarRegistroValido(request);
            request.setGoogleScholar("https://ejemplo.com/perfil");
            request.setOrcid("ORCID inválido");

            assertThat(factory.getValidator().validate(request)).hasSize(2);
        }
    }

    private void completarRegistroValido(RegistroUsuarioDTO request) {
        request.setNombreCompleto("María Pérez");
        request.setCorreoInstitucional("maria.perez@uteq.edu.ec");
        request.setCargoId(1L);
    }

    private PerfilAcademicoRequestDTO requestValido() {
        PerfilAcademicoRequestDTO request = new PerfilAcademicoRequestDTO();
        request.setGoogleScholar("https://scholar.google.com/citations?user=abc123");
        request.setOrcid("https://orcid.org/0000-0002-1825-0097");
        return request;
    }
}
// FIN - Perfil Académico
