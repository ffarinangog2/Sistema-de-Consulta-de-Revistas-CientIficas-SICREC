package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.crecuteq.entity.SpringerRevista;
import uteq.edu.ec.crecuteq.repository.SpringerRevistaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// INICIO - Integración Springer en búsquedas
class SpringerRevistaBusquedaTests {

    @Test
    void respetaPrioridadProductIdIssnYEissn() {
        SpringerRevistaRepository repository =
                mock(SpringerRevistaRepository.class);
        SpringerRevista encontrada = new SpringerRevista();
        encontrada.setTitulo("Revista Springer");
        encontrada.setProductId("J123");
        when(repository.findFirstByProductIdIgnoreCase("J123"))
                .thenReturn(Optional.of(encontrada));

        var resultado = new SpringerRevistaService(repository)
                .buscarParaBusqueda(" J123 ", "1234-5679", "2049-3630");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Revista Springer");
        verify(repository).findFirstByProductIdIgnoreCase("J123");
        verify(repository, never()).findFirstByIssnNormalizado("12345679");
        verify(repository, never()).findFirstByEissnNormalizado("20493630");
    }

    @Test
    void usaEissnNormalizadoComoUltimoRecurso() {
        SpringerRevistaRepository repository =
                mock(SpringerRevistaRepository.class);
        SpringerRevista encontrada = new SpringerRevista();
        encontrada.setTitulo("Coincidencia eISSN");
        encontrada.setApcEur("2,590");
        encontrada.setApcUsd("3,190");
        encontrada.setApcGbp("2,290");
        encontrada.setApcWebsite("https://link.springer.com/journal/10955");
        encontrada.setNumerosPorVolumen(12);
        encontrada.setNumerosProgramados(2);
        encontrada.setComentarios("No longer published by Springer");
        when(repository.findFirstByIssnNormalizado("12345679"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByEissnNormalizado("20493630"))
                .thenReturn(Optional.of(encontrada));

        var resultado = new SpringerRevistaService(repository)
                .buscarParaBusqueda(null, "1234-5679", "2049-3630");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getApcEur()).isEqualTo("2,590");
        assertThat(resultado.get().getApcUsd()).isEqualTo("3,190");
        assertThat(resultado.get().getApcGbp()).isEqualTo("2,290");
        assertThat(resultado.get().getApcWebsite())
                .isEqualTo("https://link.springer.com/journal/10955");
        assertThat(resultado.get().getPeriodicidadEstimada()).isEqualTo("Mensual");
        assertThat(resultado.get().getEstadoSpringer())
                .isEqualTo("Descontinuada en Springer");
        assertThat(resultado.get().getNumerosPorVolumen()).isEqualTo(12);
        assertThat(resultado.get().getNumerosProgramados()).isEqualTo(2);
        assertThat(resultado.get().getComentarios())
                .isEqualTo("No longer published by Springer");
        verify(repository).findFirstByIssnNormalizado("12345679");
        verify(repository).findFirstByEissnNormalizado("20493630");
    }

    @Test
    void usaFrecuenciaGenericaYEstadoActivoSinComentarioDeDescontinuacion() {
        SpringerRevistaRepository repository = mock(SpringerRevistaRepository.class);
        SpringerRevista encontrada = new SpringerRevista();
        encontrada.setTitulo("Revista activa");
        encontrada.setNumerosPorVolumen(3);
        encontrada.setComentarios("Published for the society");
        when(repository.findFirstByEissnNormalizado("20493630"))
                .thenReturn(Optional.of(encontrada));

        var resultado = new SpringerRevistaService(repository)
                .buscarParaBusqueda(null, null, "2049-3630")
                .orElseThrow();

        assertThat(resultado.getPeriodicidadEstimada())
                .isEqualTo("Frecuencia: 3 números por volumen");
        assertThat(resultado.getEstadoSpringer()).isEqualTo("Activa en Springer");
    }
}
// FIN - Integración Springer en búsquedas
