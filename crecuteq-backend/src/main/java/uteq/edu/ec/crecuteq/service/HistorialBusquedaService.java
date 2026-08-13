package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.HistorialBusqueda;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.HistorialBusquedaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistorialBusquedaService {

    private final HistorialBusquedaRepository historialRepository;
    private final AuditoriaService auditoriaService;

    public HistorialBusquedaService(
            HistorialBusquedaRepository historialRepository,
            AuditoriaService auditoriaService
    ) {

        this.historialRepository = historialRepository;
        this.auditoriaService = auditoriaService;
    }

    public void guardarBusqueda(
            Usuario usuario,
            String termino,
            Integer cantidadResultados
    ) {

        // INICIO - Auditoría módulo Búsquedas
        try {
            HistorialBusqueda historial = new HistorialBusqueda();

            historial.setUsuario(usuario);
            historial.setTerminoBusqueda(termino);
            historial.setFechaBusqueda(LocalDateTime.now());
            historial.setCantidadResultados(cantidadResultados);

            historialRepository.save(historial);

            auditoriaService.registrarExito(
                    usuario,
                    "BÚSQUEDAS",
                    "BÚSQUEDA_REALIZADA",
                    "Término buscado: "
                            + termino
                            + ". Resultados encontrados: "
                            + cantidadResultados
            );
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    usuario,
                    "BÚSQUEDAS",
                    "BÚSQUEDA_REALIZADA",
                    "No fue posible registrar la búsqueda del término: "
                            + termino
            );
            throw e;
        }
        // FIN - Auditoría módulo Búsquedas
    }

    public List<HistorialBusqueda> listarPorUsuario(Usuario usuario) {

        return historialRepository
                .findByUsuarioOrderByFechaBusquedaDesc(usuario);

    }

    // INICIO - Auditoría módulo Búsquedas
    public void registrarBusquedaFallida(
            Usuario usuario,
            String termino
    ) {

        auditoriaService.registrarError(
                usuario,
                "BÚSQUEDAS",
                "BÚSQUEDA_REALIZADA",
                "La búsqueda del término no pudo completarse: "
                        + termino
        );
    }
    // FIN - Auditoría módulo Búsquedas

    public void eliminar(Long id, Usuario usuarioActual, boolean administrador) {

        HistorialBusqueda historial = historialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Registro de historial no encontrado"));
        if (!administrador && !usuarioActual.getId().equals(historial.getUsuario().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede eliminar el historial de otro usuario");
        }
        historialRepository.delete(historial);

    }
}
