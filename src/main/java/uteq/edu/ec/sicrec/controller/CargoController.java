package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.entity.Cargo;
import uteq.edu.ec.sicrec.service.CargoService;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
@CrossOrigin(origins = "*")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @GetMapping
    public List<Cargo> listarCargos() {
        return cargoService.listarCargos();
    }

    @GetMapping("/{id}")
    public Cargo buscarCargo(@PathVariable Long id) {

        return cargoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));

    }

    @PostMapping
    public Cargo crearCargo(@RequestBody Cargo cargo) {

        return cargoService.guardarCargo(cargo);

    }

    @PutMapping("/{id}")
    public Cargo actualizarCargo(
            @PathVariable Long id,
            @RequestBody Cargo cargo) {

        return cargoService.actualizarCargo(id, cargo);

    }

    @DeleteMapping("/{id}")
    public void eliminarCargo(@PathVariable Long id) {

        cargoService.eliminarCargo(id);

    }

}