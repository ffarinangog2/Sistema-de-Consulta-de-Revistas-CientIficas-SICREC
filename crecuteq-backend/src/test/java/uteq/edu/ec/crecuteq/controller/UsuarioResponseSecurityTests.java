package uteq.edu.ec.crecuteq.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.crecuteq.dto.UsuarioResponseDTO;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioResponseSecurityTests {

    @Test
    void respuestaDeUsuarioNoExponePassword() throws Exception {
        UsuarioResponseDTO response = new UsuarioResponseDTO(
                1L, "Usuario Seguro", "seguro@uteq.edu.ec", true, "USUARIO", "Docente");

        String json = new ObjectMapper().writeValueAsString(response);

        assertFalse(json.toLowerCase().contains("password"));
        assertTrue(json.contains("seguro@uteq.edu.ec"));
    }
}
