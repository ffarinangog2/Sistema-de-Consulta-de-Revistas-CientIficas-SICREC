package uteq.edu.ec.sicrec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.sicrec.entity.Scimago;

import java.util.Optional;

@Repository
public interface ScimagoRepository extends JpaRepository<Scimago, Double> {

    Optional<Scimago> findBySourceid(Double sourceid);
    Optional<Scimago> findByIssnContaining(String issn);

}