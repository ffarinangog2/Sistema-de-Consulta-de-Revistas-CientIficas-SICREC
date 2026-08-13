package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.Favorito;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.FavoritoRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public FavoritoService(
            FavoritoRepository favoritoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService
    ) {

        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public Favorito guardar(Favorito favorito) {

        // INICIO - Auditoría módulo Favoritos
        Usuario usuario = obtenerUsuario(favorito.getUsuarioId());

        try {
            if (favoritoRepository
                    .findByUsuarioIdAndSourceId(
                            favorito.getUsuarioId(),
                            favorito.getSourceId()
                    )
                    .isPresent()) {

                throw new RuntimeException(
                        "La revista ya está en favoritos."
                );
            }

            favorito.setFechaGuardado(LocalDateTime.now());

            Favorito favoritoGuardado = favoritoRepository.save(favorito);

            auditoriaService.registrarExito(
                    usuario,
                    "FAVORITOS",
                    "FAVORITO_AGREGADO",
                    "Favorito agregado: "
                            + descripcionFavorito(favoritoGuardado)
            );

            return favoritoGuardado;
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    usuario,
                    "FAVORITOS",
                    "FAVORITO_AGREGADO",
                    "No fue posible agregar el favorito"
            );
            throw e;
        }
        // FIN - Auditoría módulo Favoritos

    }

    public List<Favorito> listar(Long usuarioId) {

        return favoritoRepository.findByUsuarioId(usuarioId);

    }

    public void eliminar(Long id, Usuario usuarioActual, boolean administrador) {

        // INICIO - Auditoría módulo Favoritos
        Usuario usuario = null;

        try {
            Favorito favorito = favoritoRepository.findById(id)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Favorito no encontrado"));

            if (!administrador && !usuarioActual.getId().equals(favorito.getUsuarioId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No puede eliminar favoritos de otro usuario");
            }

            usuario = obtenerUsuario(favorito.getUsuarioId());

            favoritoRepository.delete(favorito);

            auditoriaService.registrarExito(
                    usuario,
                    "FAVORITOS",
                    "FAVORITO_ELIMINADO",
                    "Favorito eliminado: "
                            + descripcionFavorito(favorito)
            );
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    usuario,
                    "FAVORITOS",
                    "FAVORITO_ELIMINADO",
                    "No fue posible eliminar el favorito con id " + id
            );
            throw e;
        }
        // FIN - Auditoría módulo Favoritos

    }

    private Usuario obtenerUsuario(Long usuarioId) {

        if (usuarioId == null) {
            return null;
        }

        return usuarioRepository.findById(usuarioId).orElse(null);
    }

    private String descripcionFavorito(Favorito favorito) {

        return favorito.getTitulo() != null
                && !favorito.getTitulo().isBlank()
                ? favorito.getTitulo()
                : favorito.getSourceId();
    }

}
