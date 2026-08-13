package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.SageRevista;
import java.util.Optional;

@Repository
public interface SageRevistaRepository extends JpaRepository<SageRevista, Long> {
    Optional<SageRevista> findFirstByIssnNormalizadoOrEissnNormalizado(String issn, String eissn);
    Optional<SageRevista> findFirstByJournalCodeIgnoreCaseOrTlaIgnoreCase(String journalCode, String tla);
    Optional<SageRevista> findFirstByModeloPublicacionAndJournalCodeIgnoreCase(String modelo, String journalCode);
}
