package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.projection.PerfilProjection;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    Optional<Usuario> findByUsuario(String usuario);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    boolean existsByUsuario(String usuario);

    boolean existsByCargoId(Long cargoId);

    Optional<Usuario> findByCorreoInstitucionalAndPassword(
            String correoInstitucional,
            String password
    );

    @Query(
            value = """
                    SELECT *
                    FROM fn_obtener_perfil(:idUsuario)
                    """,
            nativeQuery = true
    )
    PerfilProjection obtenerPerfil(
            @Param("idUsuario") Long idUsuario
    );

}
