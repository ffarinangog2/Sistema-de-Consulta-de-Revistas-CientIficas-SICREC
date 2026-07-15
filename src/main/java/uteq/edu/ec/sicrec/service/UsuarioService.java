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
import java.util.List;
import java.util.Optional;
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

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolRepository rolRepository,
            CargoRepository cargoRepository,
            EmailService emailService,
            TokenRecuperacionRepository tokenRecuperacionRepository)
    {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolRepository = rolRepository;
        this.cargoRepository = cargoRepository;
        this.emailService =emailService;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
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

        String usuarioGenerado =
                (partes[0].substring(0, 1) + partes[partes.length - 1])
                        .toLowerCase();

        String usuarioFinal = usuarioGenerado;

        int contador = 2;

        while (usuarioRepository.existsByUsuario(usuarioFinal)) {

            usuarioFinal = usuarioGenerado + contador;
            contador++;

        }

        usuario.setUsuario(usuarioFinal);


        String passwordTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

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
                "http://localhost:5173/restablecer-password?token="
                        + token;

        emailService.enviarRecuperacionPassword(
                usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto(),
                enlace
        );
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

        Usuario usuario = tokenRecuperacion.getUsuario();

        usuario.setPassword(
                passwordEncoder.encode(nuevaPassword)
        );
        usuario.setDebeCambiarPassword(false);

        usuarioRepository.save(usuario);

        tokenRecuperacionRepository.deleteByToken(token);
    }

    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombreCompleto(usuarioActualizado.getNombreCompleto());
        usuario.setCorreoInstitucional(usuarioActualizado.getCorreoInstitucional());
        usuario.setEstado(usuarioActualizado.getEstado());
        usuario.setRol(usuarioActualizado.getRol());
        usuario.setCargo(usuarioActualizado.getCargo());

        return usuarioRepository.save(usuario);
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

        Usuario usuario = usuarioRepository
                .findByUsuario(request.getUsuario())
                .orElseThrow(() ->
                        new RuntimeException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword())) {

            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generarToken(
                request.getUsuario()
        );

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

        usuario.setPassword(
                passwordEncoder.encode(dto.getNuevaPassword())
        );
usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

    }


}
