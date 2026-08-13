package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.SpringerRevista;

import java.util.Optional;

// INICIO - Módulo Springer
@Repository
public interface SpringerRevistaRepository extends JpaRepository<SpringerRevista, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(revista) > 0 THEN true ELSE false END
            FROM SpringerRevista revista
            WHERE (:issn IS NOT NULL AND
                   (revista.issnNormalizado = :issn OR revista.eissnNormalizado = :issn))
               OR (:eissn IS NOT NULL AND
                   (revista.issnNormalizado = :eissn OR revista.eissnNormalizado = :eissn))
               OR (:productId IS NOT NULL AND LOWER(revista.productId) = LOWER(:productId))
            """)
    boolean existeDuplicado(
            @Param("issn") String issn,
            @Param("eissn") String eissn,
            @Param("productId") String productId
    );

    @Query("""
            SELECT CASE WHEN COUNT(revista) > 0 THEN true ELSE false END
            FROM SpringerRevista revista
            WHERE revista.id <> :id
              AND (
                    (:issn IS NOT NULL AND
                     (revista.issnNormalizado = :issn OR revista.eissnNormalizado = :issn))
                 OR (:eissn IS NOT NULL AND
                     (revista.issnNormalizado = :eissn OR revista.eissnNormalizado = :eissn))
                 OR (:productId IS NOT NULL AND LOWER(revista.productId) = LOWER(:productId))
              )
            """)
    boolean existeDuplicadoExcluyendoId(
            @Param("id") Long id,
            @Param("issn") String issn,
            @Param("eissn") String eissn,
            @Param("productId") String productId
    );

    // INICIO - Importación Springer
    Optional<SpringerRevista> findFirstByProductIdIgnoreCase(String productId);

    Optional<SpringerRevista> findFirstByIssnNormalizado(String issnNormalizado);

    Optional<SpringerRevista> findFirstByEissnNormalizado(String eissnNormalizado);
    // FIN - Importación Springer
}
// FIN - Módulo Springer
