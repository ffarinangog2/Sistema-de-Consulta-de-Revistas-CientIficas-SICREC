package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.crecuteq.dto.SpringerApcRequestDTO;
import uteq.edu.ec.crecuteq.entity.SpringerRevista;
import uteq.edu.ec.crecuteq.repository.SpringerRevistaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// INICIO - Importación PDF Springer
class SpringerApcUpdateTests {

    @Test
    void actualizaSoloApcBuscandoPrimeroPorEissn() {
        SpringerRevistaRepository repository =
                mock(SpringerRevistaRepository.class);
        SpringerRevista revista = new SpringerRevista();
        revista.setTitulo("Título Excel");
        revista.setImprint("Imprint Excel");
        revista.setUrlOficial("https://excel.example");
        when(repository.findFirstByEissnNormalizado("23656271"))
                .thenReturn(Optional.of(revista));

        SpringerApcRequestDTO request = new SpringerApcRequestDTO();
        request.setTitulo("Título PDF distinto");
        request.setImprint("Imprint PDF distinto");
        request.setEissn("2365-6271");
        request.setIssn("1234-5679");
        request.setApcEur("see website");
        request.setApcUsd("");
        request.setApcGbp("texto especial");
        request.setUrlOficial("https://apc.example");
        request.setAnioVigencia(2026);

        boolean actualizado =
                new SpringerRevistaService(repository).actualizarApc(request);

        assertThat(actualizado).isTrue();
        assertThat(revista.getTitulo()).isEqualTo("Título Excel");
        assertThat(revista.getImprint()).isEqualTo("Imprint Excel");
        assertThat(revista.getUrlOficial()).isEqualTo("https://excel.example");
        assertThat(revista.getApcEur()).isEqualTo("see website");
        assertThat(revista.getApcUsd()).isEmpty();
        assertThat(revista.getApcGbp()).isEqualTo("texto especial");
        assertThat(revista.getApcWebsite()).isEqualTo("https://apc.example");
        verify(repository, never()).findFirstByIssnNormalizado("12345679");
        verify(repository).saveAndFlush(revista);
    }

    @Test
    void usaIssnCuandoNoExisteCoincidenciaPorEissn() {
        SpringerRevistaRepository repository =
                mock(SpringerRevistaRepository.class);
        SpringerRevista revista = new SpringerRevista();
        revista.setTitulo("Revista");
        when(repository.findFirstByEissnNormalizado("23656271"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByIssnNormalizado("12345679"))
                .thenReturn(Optional.of(revista));

        SpringerApcRequestDTO request = new SpringerApcRequestDTO();
        request.setEissn("2365-6271");
        request.setIssn("1234-5679");

        assertThat(new SpringerRevistaService(repository).actualizarApc(request))
                .isTrue();
        verify(repository).findFirstByEissnNormalizado("23656271");
        verify(repository).findFirstByIssnNormalizado("12345679");
    }
}
// FIN - Importación PDF Springer
