package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.DoajRevista;

import java.util.Optional;

// INICIO - Repositorio DOAJ reutilizable por ETL y estrategia
@Repository
public interface DoajRevistaRepository extends JpaRepository<DoajRevista, Long> {
    Optional<DoajRevista> findFirstByIssnNormalizado(String issn);
    Optional<DoajRevista> findFirstByEissnNormalizado(String eissn);
}
// FIN - Repositorio DOAJ reutilizable por ETL y estrategia
