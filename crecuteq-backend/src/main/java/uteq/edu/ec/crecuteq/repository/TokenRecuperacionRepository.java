package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.TokenRecuperacion;
import uteq.edu.ec.crecuteq.entity.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByToken(String token);

    Optional<TokenRecuperacion> findByTokenAndFechaExpiracionAfter(
            String token,
            LocalDateTime fechaActual
    );

    // Elimina el token después de restablecer la contraseña.
    void deleteByToken(String token);

    void deleteByUsuario(Usuario usuario);

}
