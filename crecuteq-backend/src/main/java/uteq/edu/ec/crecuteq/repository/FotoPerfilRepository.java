package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.crecuteq.entity.FotoPerfil;
import java.util.Optional;

public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, Long> {
    Optional<FotoPerfil> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
}
