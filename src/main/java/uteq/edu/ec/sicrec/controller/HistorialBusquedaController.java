package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.entity.HistorialBusqueda;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.repository.UsuarioRepository;
import uteq.edu.ec.sicrec.service.HistorialBusquedaService;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
@CrossOrigin(origins = "*")
public class HistorialBusquedaController {

    private final HistorialBusquedaService historialService;
    private final UsuarioRepository usuarioRepository;

    public HistorialBusquedaController(
            HistorialBusquedaService historialService,
            UsuarioRepository usuarioRepository
    ) {
        this.historialService = historialService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/{usuarioId}")
    public List<HistorialBusqueda> listar(@PathVariable Long usuarioId) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();

        return historialService.listarPorUsuario(usuario);

    }
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {

        historialService.eliminar(id);

    }
}