package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.IeeeRevista;
import java.util.Optional;

@Repository
public interface IeeeRevistaRepository extends JpaRepository<IeeeRevista, Long> {
    Optional<IeeeRevista> findFirstByAcronimoIgnoreCase(String acronimo);
    Optional<IeeeRevista> findFirstByIssnNormalizadoOrEissnNormalizado(String issn, String eissn);
}
