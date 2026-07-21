package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "usuarioId debe ser un identificador válido."
            );
        }

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el usuario indicado."
                ));

        // INICIO - Auditoría módulo Búsquedas
        List<RevistaDTO> resultado;

        try {
            resultado = revistaService.buscarRevistas(
                    termino,
                    cantidad
            );
        } catch (RuntimeException e) {
            historialBusquedaService.registrarBusquedaFallida(
                    usuario,
                    termino
            );
            throw e;
        }
        // FIN - Auditoría módulo Búsquedas

        historialBusquedaService.guardarBusqueda(
                usuario,
                termino,
                resultado.size()
        );

        return resultado;

    }

}
