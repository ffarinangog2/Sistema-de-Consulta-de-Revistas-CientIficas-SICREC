package uteq.edu.ec.sicrec.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.sicrec.dto.RevistaDTO;
import uteq.edu.ec.sicrec.dto.SerialTitleDTO;
import uteq.edu.ec.sicrec.repository.ScimagoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RevistaServiceTests {

    private MockRestServiceServer server;
    private RevistaService service;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        ScimagoRepository scimagoRepository = mock(ScimagoRepository.class);
        when(scimagoRepository.findBySourceid(org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(Optional.empty());
        when(scimagoRepository.findByIssnContaining(anyString()))
                .thenReturn(Optional.empty());

        SerialTitleService serialTitleService = mock(SerialTitleService.class);
        SerialTitleDTO metricas = new SerialTitleDTO();
        metricas.setPercentile("88");
        metricas.setQuartile("Q1");
        metricas.setBestPercentile("88");
        metricas.setBestQuartile("Q1");
        when(serialTitleService.obtenerMetricas(anyString())).thenReturn(metricas);

        service = new RevistaService(builder.build(), scimagoRepository, serialTitleService);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(
                service,
                "serialUrl",
                "https://api.elsevier.com/content/serial/title"
        );
    }

    @Test
    void mapeaCamposOficialesSinEliminarCamposExistentes() {
        server.expect(requestTo(containsString("issn=03088146")))
                .andRespond(withSuccess("""
                        {
                          "serial-metadata-response": {
                            "entry": [{
                              "dc:title": "Food Chemistry",
                              "dc:publisher": "Elsevier Ltd.",
                              "source-id": "24039",
                              "prism:issn": "0308-8146",
                              "prism:eIssn": "1873-7072",
                              "prism:aggregationType": "journal",
                              "openaccess": "1",
                              "openaccessType": "Full",
                              "coverageStartYear": "1976",
                              "coverageEndYear": "2025",
                              "subject-area": [
                                {"@code": "1602", "@abbrev": "CHEM", "$": "Analytical Chemistry"},
                                {"@code": "1106", "@abbrev": "AGRI", "$": "Food Science"}
                              ],
                              "link": [{"@ref": "scopus-source", "@href": "https://scopus.example/source/24039"}]
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas("0308-8146", 5);

        assertThat(resultado).hasSize(1);
        RevistaDTO revista = resultado.get(0);
        assertThat(revista.getTitulo()).isEqualTo("Food Chemistry");
        assertThat(revista.getCuartil()).isEqualTo("Q1");
        assertThat(revista.getScopus().getPublisher()).isEqualTo("Elsevier Ltd.");
        assertThat(revista.getScopus().getTipoFuente()).isEqualTo("journal");
        assertThat(revista.getScopus().getAccesoAbierto()).isTrue();
        assertThat(revista.getScopus().getTipoOpenAccess()).isEqualTo("Full");
        assertThat(revista.getScopus().getCoverageStartYear()).isEqualTo("1976");
        assertThat(revista.getScopus().getCoverageEndYear()).isEqualTo("2025");
        assertThat(revista.getScopus().getSubjectAreas()).hasSize(2);
        assertThat(revista.getScopus().getBestQuartile()).isEqualTo("Q1");
        assertThat(revista.getScopus().getPais()).isNull();
        server.verify();
    }

    @Test
    void devuelveListaVaciaCuandoScopusNoTieneResultados() {
        server.expect(requestTo(containsString("title=sin%20resultados")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.buscarRevistas("sin resultados", 25)).isEmpty();
        server.verify();
    }

    @Test
    void rechazaCantidadesFueraDelLimiteDeScopus() {
        assertThatThrownBy(() -> service.buscarRevistas("medicine", 201))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

}
