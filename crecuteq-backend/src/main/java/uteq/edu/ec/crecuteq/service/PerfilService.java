package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.dto.PerfilDTO;
import uteq.edu.ec.crecuteq.projection.PerfilProjection;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    public PerfilService(
            UsuarioRepository usuarioRepository
    ) {

        this.usuarioRepository = usuarioRepository;

    }

    public PerfilDTO obtenerPerfil(
            Long idUsuario
    ) {

        PerfilProjection perfil =
                usuarioRepository.obtenerPerfil(idUsuario);

        return convertirDTO(perfil);

    }

    private PerfilDTO convertirDTO(
            PerfilProjection projection
    ) {

        return new PerfilDTO(

                projection.getNombreCompleto(),

                projection.getCorreoInstitucional(),

                projection.getRol(),

                projection.getCargo(),

                projection.getEstado()

        );

    }

}