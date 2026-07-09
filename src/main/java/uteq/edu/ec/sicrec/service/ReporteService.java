package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.ReporteHistorialDTO;
import uteq.edu.ec.sicrec.projection.ReporteHistorialProjection;
import uteq.edu.ec.sicrec.repository.HistorialBusquedaRepository;

import java.util.List;

@Service
public class ReporteService {

    private final HistorialBusquedaRepository historialRepository;

    public ReporteService(
            HistorialBusquedaRepository historialRepository
    ) {

        this.historialRepository = historialRepository;

    }

    public List<ReporteHistorialDTO> obtenerReporteHistorial() {

        return historialRepository
                .obtenerReporteHistorial()
                .stream()
                .map(this::convertirDTO)
                .toList();

    }

    private ReporteHistorialDTO convertirDTO(
            ReporteHistorialProjection projection
    ) {

        return new ReporteHistorialDTO(

                projection.getFecha(),

                projection.getUsuario(),

                projection.getTermino(),

                projection.getResultados()

        );

    }
    public List<ReporteHistorialDTO> obtenerReporteHistorialPorFechas(
            String fechaInicio,
            String fechaFin
    ) {

        return historialRepository
                .obtenerReporteHistorialPorFechas(
                        fechaInicio,
                        fechaFin
                )
                .stream()
                .map(this::convertirDTO)
                .toList();

    }

}