package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.DashboardDTO;
import uteq.edu.ec.sicrec.dto.TopBusquedaDTO;
import uteq.edu.ec.sicrec.dto.UltimaBusquedaDTO;
import uteq.edu.ec.sicrec.projection.TopBusquedaProjection;
import uteq.edu.ec.sicrec.repository.FavoritoRepository;
import uteq.edu.ec.sicrec.repository.HistorialBusquedaRepository;
import uteq.edu.ec.sicrec.repository.ScimagoRepository;
import uteq.edu.ec.sicrec.repository.UsuarioRepository;
import uteq.edu.ec.sicrec.projection.UltimaBusquedaProjection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final HistorialBusquedaRepository historialRepository;
    private final ScimagoRepository scimagoRepository;

    public DashboardService(
            UsuarioRepository usuarioRepository,
            FavoritoRepository favoritoRepository,
            HistorialBusquedaRepository historialRepository,
            ScimagoRepository scimagoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.favoritoRepository = favoritoRepository;
        this.historialRepository = historialRepository;
        this.scimagoRepository = scimagoRepository;
    }

    public DashboardDTO obtenerResumen() {

        DashboardDTO dto = new DashboardDTO();

        dto.setTotalUsuarios(
                usuarioRepository.count()
        );

        dto.setTotalFavoritos(
                favoritoRepository.count()
        );

        dto.setTotalBusquedas(
                historialRepository.count()
        );

        dto.setTotalRevistas(
                scimagoRepository.count()
        );

        // ===============================
        // Top 10 búsquedas
        // ===============================

        List<TopBusquedaDTO> topBusquedas =
                historialRepository.obtenerTopBusquedas()
                        .stream()
                        .map(this::convertirDTO)
                        .collect(Collectors.toList());

        dto.setTopBusquedas(topBusquedas);

        List<UltimaBusquedaDTO> ultimasBusquedas =
                historialRepository.obtenerUltimasBusquedas()
                        .stream()
                        .map(this::convertirUltimaBusquedaDTO)
                        .toList();

        dto.setUltimasBusquedas(ultimasBusquedas);

        return dto;

    }

    private TopBusquedaDTO convertirDTO(
            TopBusquedaProjection projection
    ) {

        return new TopBusquedaDTO(

                projection.getTermino(),

                projection.getTotal()

        );

    }

    private UltimaBusquedaDTO convertirUltimaBusquedaDTO(
            UltimaBusquedaProjection projection
    ) {

        return new UltimaBusquedaDTO(

                projection.getFecha(),

                projection.getUsuario(),

                projection.getTermino(),

                projection.getResultados()

        );

    }

}