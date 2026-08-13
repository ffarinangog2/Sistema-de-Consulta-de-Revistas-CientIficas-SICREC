package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.dto.DashboardDTO;
import uteq.edu.ec.crecuteq.dto.TopBusquedaDTO;
import uteq.edu.ec.crecuteq.dto.UltimaBusquedaDTO;
import uteq.edu.ec.crecuteq.projection.TopBusquedaProjection;
import uteq.edu.ec.crecuteq.repository.FavoritoRepository;
import uteq.edu.ec.crecuteq.repository.HistorialBusquedaRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;
import uteq.edu.ec.crecuteq.projection.UltimaBusquedaProjection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final HistorialBusquedaRepository historialRepository;

    public DashboardService(
            UsuarioRepository usuarioRepository,
            FavoritoRepository favoritoRepository,
            HistorialBusquedaRepository historialRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.favoritoRepository = favoritoRepository;
        this.historialRepository = historialRepository;
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

        dto.setTotalBusquedasExitosas(
                historialRepository.countByCantidadResultadosGreaterThan(0)
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
