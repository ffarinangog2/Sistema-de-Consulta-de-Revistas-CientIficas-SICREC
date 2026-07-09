package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.entity.Cargo;
import uteq.edu.ec.sicrec.repository.CargoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    public List<Cargo> listarCargos() {
        return cargoRepository.findAll();
    }

    public Optional<Cargo> buscarPorId(Long id) {
        return cargoRepository.findById(id);
    }

    public Cargo guardarCargo(Cargo cargo) {
        return cargoRepository.save(cargo);
    }

    public Cargo actualizarCargo(Long id, Cargo cargoActualizado) {

        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));

        cargo.setNombreCargo(cargoActualizado.getNombreCargo());

        return cargoRepository.save(cargo);
    }

    public void eliminarCargo(Long id) {
        cargoRepository.deleteById(id);
    }

}