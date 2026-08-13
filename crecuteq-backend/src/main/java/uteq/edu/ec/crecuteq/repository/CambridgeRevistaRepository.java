package uteq.edu.ec.crecuteq.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.CambridgeRevista;
import java.util.Optional;
@Repository public interface CambridgeRevistaRepository extends JpaRepository<CambridgeRevista,Long>{
    Optional<CambridgeRevista> findFirstByMnemonicIgnoreCase(String mnemonic);
    Optional<CambridgeRevista> findFirstByIssnNormalizadoOrEissnNormalizado(String issn,String eissn);
}
