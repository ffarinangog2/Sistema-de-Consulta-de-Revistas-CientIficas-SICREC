package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.Scimago;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ScimagoRepository extends JpaRepository<Scimago, Double> {

    Optional<Scimago> findBySourceid(Double sourceid);
    Optional<Scimago> findByIssnContaining(String issn);
    List<Scimago> findAllBySourceidIn(Collection<Double> sourceids);

    @Query(value = """
            SELECT s.*
            FROM scimago s
            WHERE regexp_replace(upper(coalesce(s.\"Issn\", '')), '[^0-9X]', '', 'g')
                  ~ :patron
            """, nativeQuery = true)
    List<Scimago> findAllByIssnNormalizadoPattern(@Param("patron") String patron);

    List<Scimago> findAllByTitleIgnoreCase(String title);

}
