package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.ElsevierRevista;
import java.util.Optional;

@Repository
public interface ElsevierRevistaRepository extends JpaRepository<ElsevierRevista, Long> {
    Optional<ElsevierRevista> findFirstByIssnNormalizado(String issn);
}
