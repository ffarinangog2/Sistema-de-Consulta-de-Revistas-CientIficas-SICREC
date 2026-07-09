package uteq.edu.ec.sicrec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.sicrec.entity.Cargo;

import java.util.Optional;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    Optional<Cargo> findByNombreCargo(String nombreCargo);

}