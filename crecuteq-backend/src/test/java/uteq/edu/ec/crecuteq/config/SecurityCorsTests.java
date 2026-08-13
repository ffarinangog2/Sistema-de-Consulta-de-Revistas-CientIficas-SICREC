package uteq.edu.ec.crecuteq.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityCorsTests {

    @Test
    void corsSoloIncluyeOrigenPublicoYDesarrolloLocal() {
        SecurityConfig securityConfig = new SecurityConfig();
        var source = securityConfig.corsConfigurationSource("https://revistas.uteq.edu.ec/app");
        var request = new MockHttpServletRequest("GET", "/api/cargos");
        var configuration = source.getCorsConfiguration(request);

        assertTrue(configuration.getAllowedOrigins().contains("https://revistas.uteq.edu.ec"));
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:5173"));
        assertFalse(configuration.getAllowedOrigins().contains("https://sitio-malicioso.example"));
    }
}
