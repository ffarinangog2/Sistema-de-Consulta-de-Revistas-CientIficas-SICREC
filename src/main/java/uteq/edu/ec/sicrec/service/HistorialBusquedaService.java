package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.entity.HistorialBusqueda;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.repository.HistorialBusquedaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistorialBusquedaService {

    private final HistorialBusquedaRepository historialRepository;

    public HistorialBusquedaService(HistorialBusquedaRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    public void guardarBusqueda(
            Usuario usuario,
            String termino,
            Integer cantidadResultados
    ) {

        HistorialBusqueda historial = new HistorialBusqueda();

        historial.setUsuario(usuario);
        historial.setTerminoBusqueda(termino);
        historial.setFechaBusqueda(LocalDateTime.now());
        historial.setCantidadResultados(cantidadResultados);

        historialRepository.save(historial);

    }

    public List<HistorialBusqueda> listarPorUsuario(Usuario usuario) {

        return historialRepository
                .findByUsuarioOrderByFechaBusquedaDesc(usuario);

    }
    public void eliminar(Long id) {

        historialRepository.deleteById(id);

    }
}