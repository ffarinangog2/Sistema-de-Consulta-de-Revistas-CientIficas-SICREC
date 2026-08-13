package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.WileyRevista;
import java.util.Optional;

@Repository
public interface WileyRevistaRepository extends JpaRepository<WileyRevista, Long> {
    Optional<WileyRevista> findFirstByOnlineIssnNormalizado(String issn);
}
