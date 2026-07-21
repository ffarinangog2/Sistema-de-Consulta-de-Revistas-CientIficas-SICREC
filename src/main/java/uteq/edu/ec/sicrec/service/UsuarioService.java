package uteq.edu.ec.sicrec.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.sicrec.dto.CambiarPasswordDTO;
import uteq.edu.ec.sicrec.dto.LoginRequestDTO;
import uteq.edu.ec.sicrec.dto.LoginResponseDTO;
import uteq.edu.ec.sicrec.dto.UsuarioResponseDTO;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.entity.TokenRecuperacion;
import uteq.edu.ec.sicrec.repository.TokenRecuperacionRepository;
import uteq.edu.ec.sicrec.repository.UsuarioRepository;
import uteq.edu.ec.sicrec.security.JwtService;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import uteq.edu.ec.sicrec.repository.RolRepository;
import uteq.edu.ec.sicrec.repository.CargoRepository;
import uteq.edu.ec.sicrec.dto.RegistroUsuarioDTO;
import uteq.edu.ec.sicrec.entity.Cargo;
import uteq.edu.ec.sicrec.entity.Rol;

import java.util.UUID;
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolRepository rolRepository;
    private final CargoRepository cargoRepository;
    private final EmailService emailService;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final AuditoriaService auditoriaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolRepository rolRepository,
            CargoRepository cargoRepository,
            EmailService emailService,
            TokenRecuperacionRepository tokenRecuperacionRepository,
            AuditoriaService auditoriaService)
    {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolRepository = rolRepository;
        this.cargoRepository = cargoRepository;
        this.emailService =emailService;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.auditoriaService = auditoriaService;
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

    public Usuario registrarUsuario(RegistroUsuarioDTO dto) {

        if (!dto.getCorreoInstitucional().endsWith("@uteq.edu.ec")) {

            throw new RuntimeException(
                    "Debe utilizar el correo institucional de la UTEQ."
            );

        }

        if (usuarioRepository.existsByCorreoInstitucional(
                dto.getCorreoInstitucional())) {

            throw new RuntimeException(
                    "El correo ya se encuentra registrado."
            );

        }

        Usuario usuario = new Usuario();

        usuario.setNombreCompleto(dto.getNombreCompleto());

        usuario.setCorreoInstitucional(
                dto.getCorreoInstitucional()
        );


        String[] partes = dto.getNombreCompleto().trim().split("\\s+");

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

        Rol rol = rolRepository
                .findByNombreRol(dto.getTipoUsuario())
                .orElseThrow(() ->
                        new RuntimeException("Rol no encontrado"));

        usuario.setRol(rol);

        if (dto.getTipoUsuario().equals("DOCENTE")) {

            Cargo cargo = cargoRepository
                    .findByNombreCargo("Docente")
                    .orElseThrow(() ->
                            new RuntimeException("Cargo no encontrado"));

            usuario.setCargo(cargo);

        } else if (dto.getTipoUsuario().equals("ESTUDIANTE")) {

            Cargo cargo = cargoRepository
                    .findByNombreCargo("Estudiante")
                    .orElseThrow(() ->
                            new RuntimeException("Cargo no encontrado"));

            usuario.setCargo(cargo);

        } else {

            usuario.setCargo(null);

        }

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

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

        String enlace =
                "/restablecer-password?token="
                        + token;

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
                        new RuntimeException(
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
                        new RuntimeException(
                                "El token de recuperación no existe"
                        ));

        if (!tokenRecuperacion.getFechaExpiracion()
                .isAfter(LocalDateTime.now())) {

            throw new RuntimeException(
                    "El token de recuperación ha expirado"
            );
        }

        if (!nuevaPassword.equals(confirmarPassword)) {

            throw new RuntimeException(
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

    public void eliminarUsuario(Long id) {

        // INICIO - Desactivación lógica de usuarios
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

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

    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean estabaActivo = Boolean.TRUE.equals(usuario.getEstado());

        // INICIO - Validación de correo institucional
        if (usuarioActualizado.getCorreoInstitucional() == null
                || !usuarioActualizado.getCorreoInstitucional()
                .endsWith("@uteq.edu.ec")) {

            throw new RuntimeException(
                    "Debe utilizar el correo institucional de la UTEQ."
            );
        }
        // FIN - Validación de correo institucional

        usuario.setNombreCompleto(usuarioActualizado.getNombreCompleto());
        usuario.setCorreoInstitucional(usuarioActualizado.getCorreoInstitucional());
        usuario.setEstado(usuarioActualizado.getEstado());
        usuario.setRol(usuarioActualizado.getRol());
        usuario.setCargo(usuarioActualizado.getCargo());

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

    private UsuarioResponseDTO convertirDTO(Usuario usuario) {

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getEstado(),
                usuario.getRol().getNombreRol(),
                usuario.getCargo() != null
                        ? usuario.getCargo().getNombreCargo()
                        : null
        );
    }

    public List<UsuarioResponseDTO> listarUsuariosDTO() {

        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirDTO)
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

            throw new RuntimeException(
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

            throw new RuntimeException(
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

                throw new RuntimeException(
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

                throw new RuntimeException(
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

            throw new RuntimeException("Usuario o contraseña incorrectos");
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


    public void cambiarPassword(CambiarPasswordDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(
                dto.getPasswordActual(),
                usuario.getPassword())) {

            throw new RuntimeException("La contraseña actual es incorrecta");

        }

        if (!dto.getNuevaPassword().equals(
                dto.getConfirmarPassword())) {

            throw new RuntimeException("Las contraseñas no coinciden");

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

            throw new RuntimeException(
                    "La contraseña debe tener al menos 8 caracteres, "
                            + "una letra mayúscula, una letra minúscula y un número"
            );
        }
    }
    // FIN - Validación de contraseña


}
