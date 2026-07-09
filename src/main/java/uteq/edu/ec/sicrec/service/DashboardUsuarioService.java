package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.DashboardUsuarioDTO;
import uteq.edu.ec.sicrec.dto.FavoritoDTO;
import uteq.edu.ec.sicrec.dto.UltimaBusquedaDTO;
import uteq.edu.ec.sicrec.entity.Favorito;
import uteq.edu.ec.sicrec.entity.HistorialBusqueda;
import uteq.edu.ec.sicrec.repository.FavoritoRepository;
import uteq.edu.ec.sicrec.repository.HistorialBusquedaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardUsuarioService {

    private final FavoritoRepository favoritoRepository;
    private final HistorialBusquedaRepository historialRepository;

    public DashboardUsuarioService(
            FavoritoRepository favoritoRepository,
            HistorialBusquedaRepository historialRepository
    ) {

        this.favoritoRepository = favoritoRepository;
        this.historialRepository = historialRepository;

    }

    public DashboardUsuarioDTO obtenerDashboardUsuario(
            Long idUsuario
    ) {

        DashboardUsuarioDTO dto = new DashboardUsuarioDTO();

        dto.setTotalFavoritos(
                favoritoRepository.countByUsuarioId(idUsuario)
        );

        dto.setTotalBusquedas(
                historialRepository.countByUsuario_Id(idUsuario)
        );

        List<FavoritoDTO> favoritos =
                favoritoRepository
                        .findTop5ByUsuarioIdOrderByFechaGuardadoDesc(idUsuario)
                        .stream()
                        .map(this::convertirFavoritoDTO)
                        .collect(Collectors.toList());

        dto.setUltimosFavoritos(favoritos);

        List<UltimaBusquedaDTO> busquedas =
                historialRepository
                        .findTop5ByUsuario_IdOrderByFechaBusquedaDesc(idUsuario)
                        .stream()
                        .map(this::convertirBusquedaDTO)
                        .collect(Collectors.toList());

        dto.setUltimasBusquedas(busquedas);

        return dto;

    }

    private FavoritoDTO convertirFavoritoDTO(
            Favorito favorito
    ) {

        return new FavoritoDTO(

                favorito.getTitulo(),

                favorito.getRevista(),

                favorito.getCuartil(),

                favorito.getAnio(),

                favorito.getFechaGuardado()

        );

    }

    private UltimaBusquedaDTO convertirBusquedaDTO(
            HistorialBusqueda historial
    ) {

        return new UltimaBusquedaDTO(

                historial.getFechaBusqueda(),

                historial.getUsuario().getNombreCompleto(),

                historial.getTerminoBusqueda(),

                historial.getCantidadResultados()

        );

    }

}