package uteq.edu.ec.crecuteq.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.FotoPerfil;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.FotoPerfilRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.security.JwtService;

import java.io.IOException;
import java.util.Set;

@Service
public class FotoPerfilService {
    private static final String BEARER = "Bearer ";
    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final FotoPerfilRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public FotoPerfilService(FotoPerfilRepository repository,
                             UsuarioRepository usuarioRepository,
                             JwtService jwtService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public FotoPerfil obtener(String authorization) {
        Usuario usuario = usuario(authorization);
        return repository.findByUsuarioId(usuario.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no tiene foto de perfil"));
    }

    @Transactional
    public void guardar(String authorization, MultipartFile archivo) {
        Usuario usuario = usuario(authorization);
        validar(archivo);
        FotoPerfil foto = repository.findByUsuarioId(usuario.getId()).orElseGet(FotoPerfil::new);
        try {
            foto.setUsuario(usuario);
            foto.setContenido(archivo.getBytes());
            foto.setTipoContenido(archivo.getContentType());
            repository.save(foto);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No fue posible leer la imagen seleccionada", exception);
        }
    }

    @Transactional
    public void eliminar(String authorization) {
        repository.deleteByUsuarioId(usuario(authorization).getId());
    }

    private Usuario usuario(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        String subject;
        try {
            subject = jwtService.obtenerSubject(authorization.substring(BEARER.length()).trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage());
        }
        return usuarioRepository.findByUsuario(subject).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe"));
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione una imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La imagen no puede superar los 5 MB");
        }
        if (!TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Utilice una imagen JPG, PNG o WebP");
        }
    }
}
