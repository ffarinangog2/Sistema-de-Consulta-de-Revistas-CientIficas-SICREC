package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.entity.Cargo;
import uteq.edu.ec.sicrec.repository.CargoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;
    private final AuditoriaService auditoriaService;

    public CargoService(
            CargoRepository cargoRepository,
            AuditoriaService auditoriaService
    ) {

        this.cargoRepository = cargoRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Cargo> listarCargos() {
        return cargoRepository.findAll();
    }

    public Optional<Cargo> buscarPorId(Long id) {
        return cargoRepository.findById(id);
    }

    public Cargo guardarCargo(Cargo cargo) {

        // INICIO - Auditoría módulo Cargos
        try {
            Cargo cargoGuardado = cargoRepository.save(cargo);

            auditoriaService.registrarExito(
                    null,
                    "CARGOS",
                    "CARGO_CREADO",
                    "Cargo creado: " + cargoGuardado.getNombreCargo()
            );

            return cargoGuardado;
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "CARGOS",
                    "CARGO_CREADO",
                    "No fue posible crear el cargo"
            );
            throw e;
        }
        // FIN - Auditoría módulo Cargos
    }

    public Cargo actualizarCargo(Long id, Cargo cargoActualizado) {

        // INICIO - Auditoría módulo Cargos
        try {
            Cargo cargo = cargoRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Cargo no encontrado"));

            cargo.setNombreCargo(cargoActualizado.getNombreCargo());

            Cargo cargoGuardado = cargoRepository.save(cargo);

            auditoriaService.registrarExito(
                    null,
                    "CARGOS",
                    "CARGO_EDITADO",
                    "Cargo editado: " + cargoGuardado.getNombreCargo()
            );

            return cargoGuardado;
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "CARGOS",
                    "CARGO_EDITADO",
                    "No fue posible editar el cargo con id " + id
            );
            throw e;
        }
        // FIN - Auditoría módulo Cargos
    }

    public void eliminarCargo(Long id) {

        // INICIO - Auditoría módulo Cargos
        try {
            Cargo cargo = cargoRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Cargo no encontrado"));

            cargoRepository.delete(cargo);

            auditoriaService.registrarExito(
                    null,
                    "CARGOS",
                    "CARGO_ELIMINADO",
                    "Cargo eliminado: " + cargo.getNombreCargo()
            );
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "CARGOS",
                    "CARGO_ELIMINADO",
                    "No fue posible eliminar el cargo con id " + id
            );
            throw e;
        }
        // FIN - Auditoría módulo Cargos
    }

}
