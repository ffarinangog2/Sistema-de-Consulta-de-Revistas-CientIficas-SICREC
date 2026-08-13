package uteq.edu.ec.crecuteq.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoRequestDTO;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoResponseDTO;
import uteq.edu.ec.crecuteq.entity.PerfilAcademico;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.PerfilAcademicoRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.security.JwtService;

// INICIO - Perfil Académico
@Service
public class PerfilAcademicoService {

    private static final String BEARER_PREFIX = "Bearer ";
    private final PerfilAcademicoRepository perfilAcademicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public PerfilAcademicoService(PerfilAcademicoRepository perfilAcademicoRepository,
                                  UsuarioRepository usuarioRepository,
                                  JwtService jwtService) {
        this.perfilAcademicoRepository = perfilAcademicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public PerfilAcademicoResponseDTO obtener(String authorizationHeader) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        return perfilAcademicoRepository.findByUsuarioId(usuario.getId())
                .map(this::convertirDTO)
                .orElseGet(() -> perfilVacio(usuario.getId()));
    }

    @Transactional
    public PerfilAcademicoResponseDTO crear(String authorizationHeader,
                                             PerfilAcademicoRequestDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (perfilAcademicoRepository.existsByUsuarioId(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario ya tiene un Perfil Académico; utilice la actualización");
        }

        return crearParaUsuario(usuario, request);
    }

    @Transactional
    public PerfilAcademicoResponseDTO crearParaUsuario(Usuario usuario,
                                                        PerfilAcademicoRequestDTO request) {
        validarOrcid(request.getOrcid());
        PerfilAcademico perfil = new PerfilAcademico();
        perfil.setUsuario(usuario);
        aplicarDatos(perfil, request);
        return convertirDTO(perfilAcademicoRepository.save(perfil));
    }

    @Transactional
    public PerfilAcademicoResponseDTO actualizar(String authorizationHeader,
                                                  PerfilAcademicoRequestDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        PerfilAcademico perfil = perfilAcademicoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El usuario todavía no tiene un Perfil Académico; debe crearlo primero"));

        validarOrcid(request.getOrcid());
        aplicarDatos(perfil, request);
        return convertirDTO(perfilAcademicoRepository.save(perfil));
    }

    private Usuario obtenerUsuarioAutenticado(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)
                || authorizationHeader.length() <= BEARER_PREFIX.length()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Debe proporcionar un token Bearer válido");
        }

        String subject;
        try {
            subject = jwtService.obtenerSubject(
                    authorizationHeader.substring(BEARER_PREFIX.length()).trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage());
        }

        return usuarioRepository.findByUsuario(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "El usuario del token ya no existe"));
    }

    private void aplicarDatos(PerfilAcademico perfil, PerfilAcademicoRequestDTO request) {
        perfil.setGoogleScholar(normalizar(request.getGoogleScholar()));
        perfil.setOrcid(normalizar(request.getOrcid()));
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarOrcid(String orcidUrl) {
        String valor = normalizar(orcidUrl);
        if (valor == null) return;

        String identificador = valor.replace("https://orcid.org/", "")
                .replace("/", "").replace("-", "");
        if (identificador.length() != 16) throw orcidInvalido();

        int total = 0;
        for (int indice = 0; indice < 15; indice++) {
            char caracter = identificador.charAt(indice);
            if (!Character.isDigit(caracter)) throw orcidInvalido();
            total = (total + Character.digit(caracter, 10)) * 2;
        }

        int resultado = (12 - (total % 11)) % 11;
        char digitoEsperado = resultado == 10 ? 'X' : Character.forDigit(resultado, 10);
        if (identificador.charAt(15) != digitoEsperado) throw orcidInvalido();
    }

    private ResponseStatusException orcidInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El identificador ORCID no tiene un dígito de verificación válido");
    }

    private PerfilAcademicoResponseDTO convertirDTO(PerfilAcademico perfil) {
        String googleScholar = perfil.getGoogleScholar();
        String orcid = perfil.getOrcid();
        return new PerfilAcademicoResponseDTO(perfil.getId(), perfil.getUsuario().getId(),
                googleScholar, orcid, perfil.getCreatedAt(), perfil.getUpdatedAt(),
                googleScholar != null && !googleScholar.isBlank(),
                orcid != null && !orcid.isBlank());
    }

    private PerfilAcademicoResponseDTO perfilVacio(Long usuarioId) {
        return new PerfilAcademicoResponseDTO(null, usuarioId, null, null,
                null, null, false, false);
    }
}
// FIN - Perfil Académico
