package uteq.edu.ec.crecuteq.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.DegruyterRevista;
import java.util.Optional;
@Repository public interface DegruyterRevistaRepository extends JpaRepository<DegruyterRevista,Long>{
    Optional<DegruyterRevista> findFirstByCodigoOnlineIgnoreCase(String codigo);
    Optional<DegruyterRevista> findFirstByIssnNormalizadoOrEissnNormalizado(String issn,String eissn);
}
