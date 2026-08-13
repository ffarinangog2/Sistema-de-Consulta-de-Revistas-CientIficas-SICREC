package uteq.edu.ec.crecuteq.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroUsuarioValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceptaRegistroInstitucionalCompleto() {
        RegistroUsuarioDTO dto = registro("María Pérez", "maria.perez@uteq.edu.ec", 2L);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rechazaCorreoExterno() {
        RegistroUsuarioDTO dto = registro("María Pérez", "maria@gmail.com", 2L);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void rechazaCargoAusente() {
        RegistroUsuarioDTO dto = registro("María Pérez", "maria@uteq.edu.ec", null);

        assertFalse(validator.validate(dto).isEmpty());
    }

    private RegistroUsuarioDTO registro(String nombre, String correo, Long cargoId) {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setNombreCompleto(nombre);
        dto.setCorreoInstitucional(correo);
        dto.setCargoId(cargoId);
        return dto;
    }
}
