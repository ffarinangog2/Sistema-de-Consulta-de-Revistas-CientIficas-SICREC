package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.RevistaDTO;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.repository.UsuarioRepository;
import uteq.edu.ec.sicrec.service.HistorialBusquedaService;
import uteq.edu.ec.sicrec.service.RevistaService;

import java.util.List;

@RestController
@RequestMapping("/api/revistas")
@CrossOrigin(origins = "*")
public class RevistaController {

    private final RevistaService revistaService;
    private final HistorialBusquedaService historialBusquedaService;
    private final UsuarioRepository usuarioRepository;

    public RevistaController(
            RevistaService revistaService,
            HistorialBusquedaService historialBusquedaService,
            UsuarioRepository usuarioRepository
    ) {
        this.revistaService = revistaService;
        this.historialBusquedaService = historialBusquedaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/buscar")
    public List<RevistaDTO> buscar(

            @RequestParam String termino,
            @RequestParam(defaultValue = "25") Integer cantidad,
            @RequestParam Long usuarioId

    ) {

        List<RevistaDTO> resultado =
                revistaService.buscarRevistas(termino, cantidad);

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();

        historialBusquedaService.guardarBusqueda(
                usuario,
                termino,
                resultado.size()
        );

        return resultado;

    }

}