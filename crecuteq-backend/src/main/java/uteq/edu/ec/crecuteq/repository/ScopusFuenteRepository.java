package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.ScopusFuente;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ScopusFuenteRepository extends JpaRepository<ScopusFuente, Long> {
    Optional<ScopusFuente> findFirstByIssnNormalizado(String issn);
    Optional<ScopusFuente> findFirstByEissnNormalizado(String eissn);
    List<ScopusFuente> findAllByIssnNormalizadoInOrEissnNormalizadoIn(
            Collection<String> issns, Collection<String> eissns);
}
