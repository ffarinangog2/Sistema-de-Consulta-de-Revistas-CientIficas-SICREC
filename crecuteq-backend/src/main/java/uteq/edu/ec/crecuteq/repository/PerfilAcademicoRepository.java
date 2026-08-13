package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.PerfilAcademico;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

// INICIO - Perfil Académico
@Repository
public interface PerfilAcademicoRepository extends JpaRepository<PerfilAcademico, Long> {

    Optional<PerfilAcademico> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    List<PerfilAcademico> findByUsuarioIdIn(Collection<Long> usuarioIds);
}
// FIN - Perfil Académico
