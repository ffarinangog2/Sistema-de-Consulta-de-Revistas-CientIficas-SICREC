package uteq.edu.ec.crecuteq.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CambiarPasswordDTO;
import uteq.edu.ec.crecuteq.dto.LoginRequestDTO;
import uteq.edu.ec.crecuteq.dto.LoginResponseDTO;
import uteq.edu.ec.crecuteq.dto.UsuarioResponseDTO;
import uteq.edu.ec.crecuteq.dto.PerfilAcademicoRequestDTO;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.entity.TokenRecuperacion;
import uteq.edu.ec.crecuteq.repository.TokenRecuperacionRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import uteq.edu.ec.crecuteq.repository.RolRepository;
import uteq.edu.ec.crecuteq.repository.CargoRepository;
import uteq.edu.ec.crecuteq.dto.RegistroUsuarioDTO;
import uteq.edu.ec.crecuteq.entity.Cargo;
import uteq.edu.ec.crecuteq.entity.Rol;
import uteq.edu.ec.crecuteq.entity.PerfilAcademico;
import uteq.edu.ec.crecuteq.repository.PerfilAcademicoRepository;

import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
@Service
public class UsuarioService {

    @Value("${app.public-base-url:http://localhost}")
    private String publicBaseUrl;

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolRepository rolRepository;
    private final CargoRepository cargoRepository;
    private final EmailService emailService;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final AuditoriaService auditoriaService;
    private final PerfilAcademicoService perfilAcademicoService;
    private final PerfilAcademicoRepository perfilAcademicoRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolRepository rolRepository,
            CargoRepository cargoRepository,
            EmailService emailService,
            TokenRecuperacionRepository tokenRecuperacionRepository,
            AuditoriaService auditoriaService,
            PerfilAcademicoService perfilAcademicoService,
            PerfilAcademicoRepository perfilAcademicoRepository)
    {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolRepository = rolRepository;
        this.cargoRepository = cargoRepository;
        this.emailService =emailService;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.auditoriaService = auditoriaService;
        this.perfilAcademicoService = perfilAcademicoService;
        this.perfilAcademicoRepository = perfilAcademicoRepository;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreoInstitucional(correo);
    }

    public Usuario guardarUsuario(Usuario usuario) {

        usuario.setPassword(
                passwordEncoder.encode(usuario.getPassword())
        );

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario registrarUsuario(RegistroUsuarioDTO dto) {

        String nombreCompleto = dto.getNombreCompleto() == null
                ? null : dto.getNombreCompleto().trim().replaceAll("\\s+", " ");
        String correoInstitucional = dto.getCorreoInstitucional() == null
                ? null : dto.getCorreoInstitucional().trim().toLowerCase();

        if (correoInstitucional == null || !correoInstitucional.endsWith("@uteq.edu.ec")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe utilizar el correo institucional de la UTEQ."
            );

        }

        if (usuarioRepository.existsByCorreoInstitucional(
                correoInstitucional)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya se encuentra registrado."
            );

        }

        Usuario usuario = new Usuario();

        usuario.setNombreCompleto(nombreCompleto);

        usuario.setCorreoInstitucional(
                correoInstitucional
        );


        String[] partes = nombreCompleto.split("\\s+");

        // INICIO - Generación fortalecida de usuario
        String usuarioGenerado =
                (partes[0].substring(0, 1) + partes[partes.length - 1])
                        .toLowerCase();

        String usuarioFinal;

        do {

            int cantidadNumeros = ThreadLocalRandom.current()
                    .nextInt(2, 4);

            int limiteInferior = cantidadNumeros == 2 ? 10 : 100;
            int limiteSuperior = cantidadNumeros == 2 ? 100 : 1000;

            int numerosAleatorios = ThreadLocalRandom.current()
                    .nextInt(limiteInferior, limiteSuperior);

            usuarioFinal = usuarioGenerado + numerosAleatorios;

        } while (usuarioRepository.existsByUsuario(usuarioFinal));

        usuario.setUsuario(usuarioFinal);
        // FIN - Generación fortalecida de usuario


        String passwordTemporal =
                "A"
                        + UUID.randomUUID().toString().substring(0, 5)
                        + ThreadLocalRandom.current().nextInt(0, 10)
                        + "a";

        // INICIO - Validación de contraseña
        validarFortalezaPassword(passwordTemporal);
        // FIN - Validación de contraseña

        usuario.setPassword(
                passwordEncoder.encode(passwordTemporal)
        );

        usuario.setEstado(true);

        usuario.setDebeCambiarPassword(true);

        Rol rol = dto.getRolId() == null
                ? rolRepository.findByNombreRol("USUARIO")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Rol USUARIO no configurado"))
                : rolRepository.findById(dto.getRolId())
                    .filter(item -> "ADMIN".equals(item.getNombreRol()) || "USUARIO".equals(item.getNombreRol()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Rol no permitido"));

        usuario.setRol(rol);

        if (dto.getCargoId() != null) {
            usuario.setCargo(cargoRepository.findById(dto.getCargoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Cargo no encontrado")));
        } else if ("DOCENTE".equals(dto.getTipoUsuario())) {

            Cargo cargo = cargoRepository
                    .findByNombreCargo("Docente")
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo no encontrado"));

            usuario.setCargo(cargo);

        } else if ("ESTUDIANTE".equals(dto.getTipoUsuario())) {

            Cargo cargo = cargoRepository
                    .findByNombreCargo("Estudiante")
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo no encontrado"));

            usuario.setCargo(cargo);

        } else if ("PERSONAL_INVESTIGACION".equals(dto.getTipoUsuario())
                || "PERSONAL_DE_INVESTIGACION".equals(dto.getTipoUsuario())) {
            usuario.setCargo(cargoRepository.findByNombreCargo("Personal de Investigación")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Cargo no encontrado")));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo no válido");

        }

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        if ((dto.getGoogleScholar() != null && !dto.getGoogleScholar().isBlank())
                || (dto.getOrcid() != null && !dto.getOrcid().isBlank())) {
            PerfilAcademicoRequestDTO perfilRequest = new PerfilAcademicoRequestDTO();
            perfilRequest.setGoogleScholar(dto.getGoogleScholar());
            perfilRequest.setOrcid(dto.getOrcid());
            perfilAcademicoService.crearParaUsuario(usuarioGuardado, perfilRequest);
        }

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                null,
                "USUARIOS",
                "USUARIO_CREADO",
                "Usuario creado: " + usuarioGuardado.getUsuario(),
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos

        emailService.enviarPasswordTemporal(
                usuarioGuardado.getCorreoInstitucional(),
                usuarioGuardado.getNombreCompleto(),
                usuarioGuardado.getUsuario(),
                passwordTemporal
        );

        return usuarioGuardado;

    }


    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreoInstitucional(correo);
    }

    @Transactional
    public void solicitarRecuperacionPassword(String correo) {

        Optional<Usuario> usuarioOptional = usuarioRepository
                .findByCorreoInstitucional(correo);

        if (usuarioOptional.isEmpty()) {

            // INICIO - Registro de eventos
            auditoriaService.registrarEvento(
                    null,
                    "AUTENTICACIÓN",
                    "RECUPERACIÓN_SOLICITADA",
                    "Solicitud de recuperación recibida",
                    AuditoriaService.RESULTADO_EXITO
            );
            // FIN - Registro de eventos

            return;
        }

        Usuario usuario = usuarioOptional.get();

        // INICIO - Eliminación de tokens anteriores
        tokenRecuperacionRepository.deleteByUsuario(usuario);
        // FIN - Eliminación de tokens anteriores

        String token = UUID.randomUUID().toString();

        TokenRecuperacion tokenRecuperacion = new TokenRecuperacion();

        tokenRecuperacion.setToken(token);
        tokenRecuperacion.setFechaExpiracion(
                LocalDateTime.now().plusMinutes(30)
        );
        tokenRecuperacion.setUsuario(usuario);

        tokenRecuperacionRepository.save(tokenRecuperacion);

        String baseUrl = publicBaseUrl == null
                ? "http://localhost"
                : publicBaseUrl.replaceAll("/+$", "");
        String enlace = baseUrl + "/restablecer-password?token=" + token;

        emailService.enviarRecuperacionPassword(
                usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto(),
                enlace
        );

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                usuario,
                "AUTENTICACIÓN",
                "RECUPERACIÓN_SOLICITADA",
                "Solicitud de recuperación recibida",
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos
    }

    public TokenRecuperacion validarTokenRecuperacion(String token) {

        return tokenRecuperacionRepository
                .findByTokenAndFechaExpiracionAfter(
                        token,
                        LocalDateTime.now()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El token de recuperación no existe o ha expirado"
                        ));
    }

    // Restablece la contraseña y consume el token para impedir su reutilización.
    @Transactional
    public void restablecerPassword(
            String token,
            String nuevaPassword,
            String confirmarPassword
    ) {

        TokenRecuperacion tokenRecuperacion = tokenRecuperacionRepository
                .findByToken(token)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El token de recuperación no existe"
                        ));

        if (!tokenRecuperacion.getFechaExpiracion()
                .isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El token de recuperación ha expirado"
            );
        }

        if (!nuevaPassword.equals(confirmarPassword)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las contraseñas no coinciden"
            );
        }

        // INICIO - Validación de contraseña
        validarFortalezaPassword(nuevaPassword);
        // FIN - Validación de contraseña

        Usuario usuario = tokenRecuperacion.getUsuario();

        usuario.setPassword(
                passwordEncoder.encode(nuevaPassword)
        );
        usuario.setDebeCambiarPassword(false);

        usuarioRepository.save(usuario);

        tokenRecuperacionRepository.deleteByToken(token);

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                usuario,
                "AUTENTICACIÓN",
                "CONTRASEÑA_RESTABLECIDA",
                "Contraseña restablecida mediante token",
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos
    }

    public void eliminarUsuario(Long id, String authorization) {

        impedirAutogestion(id, authorization);

        // INICIO - Desactivación lógica de usuarios
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setEstado(false);

        usuarioRepository.save(usuario);

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                null,
                "USUARIOS",
                "USUARIO_DESACTIVADO",
                "Usuario desactivado: " + usuario.getUsuario(),
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos
        // FIN - Desactivación lógica de usuarios
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado, String authorization) {

        impedirAutogestion(id, authorization);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean estabaActivo = Boolean.TRUE.equals(usuario.getEstado());

        String nombreCompleto = usuarioActualizado.getNombreCompleto() == null
                ? null : usuarioActualizado.getNombreCompleto().trim().replaceAll("\\s+", " ");
        String correoInstitucional = usuarioActualizado.getCorreoInstitucional() == null
                ? null : usuarioActualizado.getCorreoInstitucional().trim().toLowerCase();

        if (nombreCompleto == null || nombreCompleto.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre completo no es válido");
        }
        // INICIO - Validación de correo institucional
        if (correoInstitucional == null || !correoInstitucional.endsWith("@uteq.edu.ec")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe utilizar el correo institucional de la UTEQ."
            );
        }
        usuarioRepository.findByCorreoInstitucional(correoInstitucional)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "El correo ya se encuentra registrado");
                });
        // FIN - Validación de correo institucional

        if (usuarioActualizado.getRol() == null || usuarioActualizado.getRol().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un rol");
        }

        usuario.setNombreCompleto(nombreCompleto);
        usuario.setCorreoInstitucional(correoInstitucional);
        usuario.setEstado(Boolean.TRUE.equals(usuarioActualizado.getEstado()));
        Rol rol = rolRepository.findById(usuarioActualizado.getRol().getId())
                .filter(item -> "ADMIN".equals(item.getNombreRol()) || "USUARIO".equals(item.getNombreRol()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Rol no permitido"));
        Cargo cargo = usuarioActualizado.getCargo() == null ? null
                : cargoRepository.findById(usuarioActualizado.getCargo().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Cargo no encontrado"));
        usuario.setRol(rol);
        usuario.setCargo(cargo);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                null,
                "USUARIOS",
                "USUARIO_ACTUALIZADO",
                "Usuario actualizado: " + usuarioGuardado.getUsuario(),
                AuditoriaService.RESULTADO_EXITO
        );

        boolean estaActivo = Boolean.TRUE.equals(usuarioGuardado.getEstado());

        if (estabaActivo != estaActivo) {

            auditoriaService.registrarEvento(
                    null,
                    "USUARIOS",
                    estaActivo
                            ? "USUARIO_ACTIVADO"
                            : "USUARIO_DESACTIVADO",
                    "Usuario "
                            + (estaActivo ? "activado: " : "desactivado: ")
                            + usuarioGuardado.getUsuario(),
                    AuditoriaService.RESULTADO_EXITO
            );
        }
        // FIN - Registro de eventos

        return usuarioGuardado;
    }

    private void impedirAutogestion(Long idObjetivo, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida");
        }
        String usuarioAutenticado;
        try {
            usuarioAutenticado = jwtService.obtenerSubject(authorization.substring(7));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticación inválido");
        }
        Usuario actual = usuarioRepository.findByUsuario(usuarioAutenticado)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "El usuario autenticado no existe"));
        if (actual.getId().equals(idObjetivo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta actual se administra desde Mi perfil y no puede desactivarse ni perder el rol ADMIN");
        }
    }

    private UsuarioResponseDTO convertirDTO(Usuario usuario, PerfilAcademico perfilAcademico) {

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getEstado(),
                usuario.getRol().getNombreRol(),
                usuario.getCargo() != null
                        ? usuario.getCargo().getNombreCargo()
                        : null,
                perfilAcademico != null ? perfilAcademico.getOrcid() : null,
                perfilAcademico != null ? perfilAcademico.getGoogleScholar() : null
        );
    }

    public UsuarioResponseDTO convertirDTO(Usuario usuario) {
        return convertirDTO(usuario, null);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuariosDTO() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        Map<Long, PerfilAcademico> perfiles = perfilAcademicoRepository
                .findByUsuarioIdIn(usuarios.stream().map(Usuario::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        perfil -> perfil.getUsuario().getId(),
                        Function.identity()
                ));

        return usuarios.stream()
                .map(usuario -> convertirDTO(usuario, perfiles.get(usuario.getId())))
                .collect(Collectors.toList());
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        Optional<Usuario> usuarioOptional = usuarioRepository
                .findByUsuario(request.getUsuario());

        if (usuarioOptional.isEmpty()) {

            // INICIO - Registro de eventos
            auditoriaService.registrarEvento(
                    null,
                    "AUTENTICACIÓN",
                    "LOGIN_FALLIDO",
                    "Intento de inicio de sesión con usuario no registrado",
                    AuditoriaService.RESULTADO_ERROR
            );
            // FIN - Registro de eventos

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña incorrectos"
            );
        }

        Usuario usuario = usuarioOptional.get();

        // INICIO - Desactivación lógica de usuarios
        if (!Boolean.TRUE.equals(usuario.getEstado())) {

            // INICIO - Registro de eventos
            auditoriaService.registrarEvento(
                    usuario,
                    "AUTENTICACIÓN",
                    "LOGIN_FALLIDO",
                    "Inicio de sesión rechazado por cuenta desactivada",
                    AuditoriaService.RESULTADO_ERROR
            );
            // FIN - Registro de eventos

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta se encuentra desactivada"
            );
        }
        // FIN - Desactivación lógica de usuarios

        // INICIO - Bloqueo temporal de cuenta
        LocalDateTime fechaActual = LocalDateTime.now();

        if (Boolean.TRUE.equals(usuario.getCuentaBloqueada())) {

            LocalDateTime fechaFinBloqueo = usuario.getFechaFinBloqueo();

            if (fechaFinBloqueo != null
                    && fechaFinBloqueo.isAfter(fechaActual)) {

                // INICIO - Registro de eventos
                auditoriaService.registrarEvento(
                        usuario,
                        "AUTENTICACIÓN",
                        "LOGIN_FALLIDO",
                        "Inicio de sesión rechazado por bloqueo temporal",
                        AuditoriaService.RESULTADO_ERROR
                );
                // FIN - Registro de eventos

                long segundosRestantes = Duration.between(
                        fechaActual,
                        fechaFinBloqueo
                ).getSeconds();

                long minutosRestantes = Math.max(
                        1,
                        (segundosRestantes + 59) / 60
                );

                throw new ResponseStatusException(
                        HttpStatus.LOCKED,
                        "La cuenta está bloqueada temporalmente. Intente nuevamente en aproximadamente "
                                + minutosRestantes
                                + " minutos"
                );
            }

            usuario.setCuentaBloqueada(false);
            usuario.setFechaFinBloqueo(null);
            usuario.setIntentosFallidos(0);

            // INICIO - Registro de eventos
            auditoriaService.registrarEvento(
                    usuario,
                    "AUTENTICACIÓN",
                    "CUENTA_DESBLOQUEADA",
                    "Cuenta desbloqueada automáticamente",
                    AuditoriaService.RESULTADO_EXITO
            );
            // FIN - Registro de eventos
        }
        // FIN - Bloqueo temporal de cuenta

        if (!passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword())) {

            // INICIO - Control de intentos de inicio de sesión
            int intentosFallidos = usuario.getIntentosFallidos() == null
                    ? 0
                    : usuario.getIntentosFallidos();

            intentosFallidos++;

            if (intentosFallidos >= 5) {

                // INICIO - Bloqueo temporal de cuenta
                usuario.setIntentosFallidos(0);
                usuario.setCuentaBloqueada(true);
                usuario.setFechaFinBloqueo(
                        LocalDateTime.now().plusMinutes(15)
                );

                usuarioRepository.save(usuario);

                // INICIO - Registro de eventos
                auditoriaService.registrarEvento(
                        usuario,
                        "AUTENTICACIÓN",
                        "LOGIN_FALLIDO",
                        "Quinto intento consecutivo de inicio de sesión fallido",
                        AuditoriaService.RESULTADO_ERROR
                );

                auditoriaService.registrarEvento(
                        usuario,
                        "AUTENTICACIÓN",
                        "CUENTA_BLOQUEADA",
                        "Cuenta bloqueada temporalmente durante 15 minutos",
                        AuditoriaService.RESULTADO_ERROR
                );
                // FIN - Registro de eventos

                throw new ResponseStatusException(
                        HttpStatus.LOCKED,
                        "La cuenta ha sido bloqueada durante 15 minutos por varios intentos fallidos"
                );
                // FIN - Bloqueo temporal de cuenta
            }

            usuario.setIntentosFallidos(intentosFallidos);
            usuarioRepository.save(usuario);

            // INICIO - Registro de eventos
            auditoriaService.registrarEvento(
                    usuario,
                    "AUTENTICACIÓN",
                    "LOGIN_FALLIDO",
                    "Intento de inicio de sesión fallido",
                    AuditoriaService.RESULTADO_ERROR
            );
            // FIN - Registro de eventos
            // FIN - Control de intentos de inicio de sesión

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña incorrectos"
            );
        }

        // INICIO - Control de intentos de inicio de sesión
        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueada(false);
        usuario.setFechaFinBloqueo(null);
        usuarioRepository.save(usuario);
        // FIN - Control de intentos de inicio de sesión

        String token = jwtService.generarToken(
                request.getUsuario()
        );

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                usuario,
                "AUTENTICACIÓN",
                "LOGIN_EXITOSO",
                "Inicio de sesión exitoso",
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getRol().getNombreRol(),
                "Login exitoso",
                token,
                usuario.getDebeCambiarPassword()
        );
    }


    public void cambiarPassword(Long usuarioAutenticadoId, CambiarPasswordDTO dto) {

        Usuario usuario = usuarioRepository.findById(usuarioAutenticadoId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (!passwordEncoder.matches(
                dto.getPasswordActual(),
                usuario.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta");

        }

        if (!dto.getNuevaPassword().equals(
                dto.getConfirmarPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");

        }

        // INICIO - Validación de contraseña
        validarFortalezaPassword(dto.getNuevaPassword());
        // FIN - Validación de contraseña

        usuario.setPassword(
                passwordEncoder.encode(dto.getNuevaPassword())
        );
usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        // INICIO - Registro de eventos
        auditoriaService.registrarEvento(
                usuario,
                "AUTENTICACIÓN",
                "CAMBIO_CONTRASEÑA",
                "Contraseña actualizada",
                AuditoriaService.RESULTADO_EXITO
        );
        // FIN - Registro de eventos

    }

    // INICIO - Validación de contraseña
    private void validarFortalezaPassword(String password) {

        boolean tieneMayuscula = password != null
                && password.chars().anyMatch(Character::isUpperCase);

        boolean tieneMinuscula = password != null
                && password.chars().anyMatch(Character::isLowerCase);

        boolean tieneNumero = password != null
                && password.chars().anyMatch(Character::isDigit);

        if (password == null
                || password.length() < 8
                || !tieneMayuscula
                || !tieneMinuscula
                || !tieneNumero) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres, "
                            + "una letra mayúscula, una letra minúscula y un número"
            );
        }
    }
    // FIN - Validación de contraseña


}
