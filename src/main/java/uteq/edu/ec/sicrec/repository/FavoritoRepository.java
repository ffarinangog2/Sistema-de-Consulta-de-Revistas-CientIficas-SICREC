package uteq.edu.ec.sicrec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.sicrec.entity.Favorito;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByUsuarioId(Long usuarioId);

    Optional<Favorito> findByUsuarioIdAndSourceId(Long usuarioId, String sourceId);


    long countByUsuarioId(Long usuarioId);

    List<Favorito> findTop5ByUsuarioIdOrderByFechaGuardadoDesc(
            Long usuarioId
    );
}