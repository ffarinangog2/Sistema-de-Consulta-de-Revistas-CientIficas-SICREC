package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.crecuteq.entity.Rol;
import uteq.edu.ec.crecuteq.service.RolService;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public List<Rol> listarRoles() {
        return rolService.listarRoles();
    }

    @GetMapping("/{id}")
    public Rol buscarRol(@PathVariable Long id) {

        return rolService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

    }

    @PostMapping
    public Rol crearRol(@RequestBody Rol rol) {

        return rolService.guardarRol(rol);

    }

    @PutMapping("/{id}")
    public Rol actualizarRol(
            @PathVariable Long id,
            @RequestBody Rol rol) {

        return rolService.actualizarRol(id, rol);

    }

    @DeleteMapping("/{id}")
    public void eliminarRol(@PathVariable Long id) {

        rolService.eliminarRol(id);

    }

}
