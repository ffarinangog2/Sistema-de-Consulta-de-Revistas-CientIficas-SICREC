package uteq.edu.ec.crecuteq.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void conservaEstadoYMensajeDeErroresControlados() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Acceso denegado", response.getBody().get("message"));
    }

    @Test
    void recursoAusenteDevuelve404() {
        var response = handler.handleNotFound(new NoSuchElementException());

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Recurso no encontrado", response.getBody().get("message"));
    }

    @Test
    void errorInesperadoNoExponeDetallesInternos() {
        var response = handler.handleUnexpected(new IllegalStateException("dato interno sensible"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("No fue posible completar la operación", response.getBody().get("message"));
    }
}
