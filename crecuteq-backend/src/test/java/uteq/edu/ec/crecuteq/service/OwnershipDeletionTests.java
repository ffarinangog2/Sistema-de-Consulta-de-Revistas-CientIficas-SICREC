package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.entity.Favorito;
import uteq.edu.ec.crecuteq.entity.HistorialBusqueda;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.repository.FavoritoRepository;
import uteq.edu.ec.crecuteq.repository.HistorialBusquedaRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OwnershipDeletionTests {

    @Test
    void usuarioNoPuedeEliminarFavoritoAjeno() {
        FavoritoRepository repository = mock(FavoritoRepository.class);
        FavoritoService service = new FavoritoService(
                repository, mock(UsuarioRepository.class), mock(AuditoriaService.class));
        Favorito favorito = new Favorito();
        favorito.setUsuarioId(9L);
        when(repository.findById(3L)).thenReturn(Optional.of(favorito));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(3L, usuario(7L), false));

        assertEquals(403, error.getStatusCode().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void usuarioNoPuedeEliminarHistorialAjeno() {
        HistorialBusquedaRepository repository = mock(HistorialBusquedaRepository.class);
        HistorialBusquedaService service = new HistorialBusquedaService(
                repository, mock(AuditoriaService.class));
        HistorialBusqueda historial = new HistorialBusqueda();
        historial.setUsuario(usuario(9L));
        when(repository.findById(3L)).thenReturn(Optional.of(historial));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(3L, usuario(7L), false));

        assertEquals(403, error.getStatusCode().value());
        verify(repository, never()).delete(any());
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        try {
            var campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return usuario;
    }
}
