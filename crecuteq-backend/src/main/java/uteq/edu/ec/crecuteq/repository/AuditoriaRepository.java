package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.Auditoria;

@Repository
public interface AuditoriaRepository extends
        JpaRepository<Auditoria, Long>,
        JpaSpecificationExecutor<Auditoria> {

}
