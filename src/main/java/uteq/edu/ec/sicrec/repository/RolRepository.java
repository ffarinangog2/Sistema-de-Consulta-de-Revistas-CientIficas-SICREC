package uteq.edu.ec.sicrec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.sicrec.entity.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

}