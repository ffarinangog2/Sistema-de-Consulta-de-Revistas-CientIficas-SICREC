package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.entity.Favorito;
import uteq.edu.ec.sicrec.service.FavoritoService;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@CrossOrigin(origins = "*")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping
    public Favorito guardar(@RequestBody Favorito favorito) {

        return favoritoService.guardar(favorito);

    }

    @GetMapping("/{usuarioId}")
    public List<Favorito> listar(
            @PathVariable Long usuarioId
    ) {

        return favoritoService.listar(usuarioId);

    }

    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Long id
    ) {

        favoritoService.eliminar(id);

    }

}