package uteq.edu.ec.crecuteq.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.BrillRevista;
import java.util.Optional;
@Repository public interface BrillRevistaRepository extends JpaRepository<BrillRevista,Long>{
    Optional<BrillRevista> findFirstByEissnNormalizado(String eissn);
    @Query("select b from BrillRevista b where b.eissnNormalizado = :issn or b.eissnNormalizado = :eissn")
    Optional<BrillRevista> buscarPorIdentificadores(@Param("issn") String issn,@Param("eissn") String eissn);
}
