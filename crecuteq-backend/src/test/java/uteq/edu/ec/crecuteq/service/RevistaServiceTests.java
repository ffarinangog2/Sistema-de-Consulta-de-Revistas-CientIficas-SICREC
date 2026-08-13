package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.RevistaDTO;
import uteq.edu.ec.crecuteq.dto.RevistaPaginaDTO;
import uteq.edu.ec.crecuteq.dto.SerialTitleDTO;
import uteq.edu.ec.crecuteq.dto.SpringerInfoDTO;
import uteq.edu.ec.crecuteq.dto.SubjectAreaDTO;
import uteq.edu.ec.crecuteq.repository.ScimagoRepository;
import uteq.edu.ec.crecuteq.repository.ScopusFuenteRepository;
import uteq.edu.ec.crecuteq.repository.FacultadScopusAreaRepository;
import uteq.edu.ec.crecuteq.entity.ScopusFuente;
import uteq.edu.ec.crecuteq.entity.Scimago;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RevistaServiceTests {

    private MockRestServiceServer server;
    private RevistaService service;
    private SpringerRevistaService springerRevistaService;
    private FacultadScopusAreaRepository facultadRepository;
    private ScimagoRepository scimagoRepository;
    private SerialTitleService serialTitleService;
    private ScopusFuenteRepository scopusFuenteRepository;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        scimagoRepository = mock(ScimagoRepository.class);
        when(scimagoRepository.findBySourceid(org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(Optional.empty());
        when(scimagoRepository.findByIssnContaining(anyString()))
                .thenReturn(Optional.empty());
        when(scimagoRepository.findAllBySourceidIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(scimagoRepository.findAllByIssnNormalizadoPattern(anyString()))
                .thenReturn(List.of());
        when(scimagoRepository.findAll()).thenReturn(List.of());

        serialTitleService = mock(SerialTitleService.class);
        SerialTitleDTO metricas = new SerialTitleDTO();
        metricas.setPercentile("88");
        metricas.setQuartile("Q1");
        metricas.setBestPercentile("88");
        metricas.setBestQuartile("Q1");
        when(serialTitleService.obtenerMetricas(anyString())).thenReturn(metricas);
        SerialTitleDTO metricasSinCuartil = new SerialTitleDTO();
        metricasSinCuartil.setCiteScore("8.5");
        metricasSinCuartil.setSjr("1.2");
        metricasSinCuartil.setSnip("1.1");
        when(serialTitleService.obtenerMetricasSinCalcularCuartil(anyString()))
                .thenReturn(metricasSinCuartil);

        springerRevistaService = mock(SpringerRevistaService.class);
        when(springerRevistaService.buscarParaBusqueda(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)
        )).thenReturn(Optional.empty());

        service = new RevistaService(
                builder.build(),
                scimagoRepository,
                serialTitleService,
                springerRevistaService
        );
        facultadRepository = mock(FacultadScopusAreaRepository.class);
        service.configurarFacultades(facultadRepository);
        scopusFuenteRepository = mock(ScopusFuenteRepository.class);
        when(scopusFuenteRepository.findAllByIssnNormalizadoInOrEissnNormalizadoIn(
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of());
        service.configurarFuentesScopus(scopusFuenteRepository);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(
                service,
                "serialUrl",
                "https://api.elsevier.com/content/serial/title"
        );
        ReflectionTestUtils.setField(service, "cacheTtlSegundos", 300L);
        ReflectionTestUtils.setField(service, "detalleCacheTtlSegundos", 600L);
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
        assertThat(revista.getSpringer()).isNull();
        server.verify();
    }

    @Test
    void agregaSpringerCuandoExisteCoincidenciaSinModificarScopus() {
        when(springerRevistaService.buscarParaBusqueda(
                "J123", "0308-8146", "1873-7072"
        )).thenReturn(Optional.of(new SpringerInfoDTO(
                "Food Chemistry Springer", "0308-8146", "1873-7072",
                "Springer", "Hybrid", "Open Choice", "English",
                null, null, null,
                "https://link.springer.com/journal/123",
                "2,590", "3,190", "2,290",
                "https://link.springer.com/journal/123/apc",
                "Mensual", "Activa en Springer", 12
        )));

        server.expect(requestTo(containsString("issn=03088146")))
                .andRespond(withSuccess("""
                        {
                          "serial-metadata-response": {
                            "entry": [{
                              "dc:title": "Food Chemistry",
                              "dc:publisher": "Editorial original",
                              "product-id": "J123",
                              "source-id": "24039",
                              "prism:issn": "0308-8146",
                              "prism:eIssn": "1873-7072"
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getScopus().getPublisher())
                .isEqualTo("Editorial original");
        assertThat(revista.getSpringer()).isNotNull();
        assertThat(revista.getSpringer().getTitulo())
                .isEqualTo("Food Chemistry Springer");
        assertThat(revista.getSpringer().getModeloPublicacion())
                .isEqualTo("Hybrid");
        assertThat(revista.getSpringer().getApcEur()).isEqualTo("2,590");
        assertThat(revista.getSpringer().getApcUsd()).isEqualTo("3,190");
        assertThat(revista.getSpringer().getApcGbp()).isEqualTo("2,290");
        assertThat(revista.getSpringer().getApcWebsite())
                .isEqualTo("https://link.springer.com/journal/123/apc");
        server.verify();
    }

    @Test
    void conservaScopusCuandoLaConsultaSpringerFalla() {
        when(springerRevistaService.buscarParaBusqueda(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)
        )).thenThrow(new IllegalStateException("Springer no disponible"));

        server.expect(requestTo(containsString("issn=03088146")))
                .andRespond(withSuccess("""
                        {
                          "serial-metadata-response": {
                            "entry": [{
                              "dc:title": "Food Chemistry",
                              "prism:issn": "0308-8146"
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getScopus()).isNotNull();
        assertThat(revista.getSpringer()).isNull();
        server.verify();
    }

    @Test
    void devuelveListaVaciaCuandoScopusNoTieneResultados() {
        server.expect(requestTo(containsString("title=inexistente")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.buscarRevistas("inexistente", 25)).isEmpty();
        server.verify();
    }

    @Test
    void tituloVariasPalabrasIgnoraAcentosConectoresYOrden() {
        server.expect(requestTo(containsString("title=arquitectura")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"1","dc:title":"Software para Arquitectura avanzada"},
                          {"source-id":"2","dc:title":"Arquitectura y Urbanismo"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=software")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"1","dc:title":"Software para Arquitectura avanzada"},
                          {"source-id":"3","dc:title":"Software Engineering"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas(
                "ARQUITECTÚRA de software", 25);

        assertThat(resultado).extracting(RevistaDTO::getTitulo)
                .startsWith("Software para Arquitectura avanzada");
        server.verify();
    }

    @Test
    void tituloAdmiteUnaPalabraYTituloParcial() {
        server.expect(requestTo(containsString("title=software")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"3","dc:title":"Software Engineering"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=architect")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"4","dc:title":"Journal of Architectural Computing"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.buscarRevistas("software", 25)).hasSize(1);
        assertThat(service.buscarRevistas("architect", 25)).hasSize(1);
        server.verify();
    }

    @Test
    void tituloExactoConservaCoincidenciaPorTodosSusTerminos() {
        server.expect(requestTo(containsString("title=software")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"5","dc:title":"Software Architecture"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=architecture")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"5","dc:title":"Software Architecture"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.buscarRevistas("Software Architecture", 25))
                .extracting(RevistaDTO::getTitulo)
                .containsExactly("Software Architecture");
        server.verify();
    }

    @Test
    void encuentraRevistaVeterinariaConTituloIncompletoYUnaRespuestaVacia() {
        String objetivo = "Revista de Investigaciones Veterinarias del Perú";
        server.expect(requestTo(containsString("title=revista")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"10","dc:title":"Revista de Investigaciones Veterinarias del Perú"},
                          {"source-id":"11","dc:title":"Revista Peruana de Medicina"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=investigaciones")))
                .andRespond(withSuccess(
                        "{\"serial-metadata-response\":{\"entry\":[]}}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=veterinarias")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"10","dc:title":"Revista de Investigaciones Veterinarias del Perú"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=per")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"10","dc:title":"Revista de Investigaciones Veterinarias del Perú"},
                          {"source-id":"12","dc:title":"Perception"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas(
                "Revista de Investigaciones Veterinarias del Per", 25);

        assertThat(resultado).isNotEmpty();
        assertThat(resultado.get(0).getTitulo()).isEqualTo(objetivo);
        server.verify();
    }

    @Test
    void toleraErrorLeveCuandoOtroTerminoObtieneElCandidato() {
        server.expect(requestTo(containsString("title=investigacones")))
                .andRespond(withSuccess(
                        "{\"serial-metadata-response\":{\"entry\":[]}}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("title=veterinarias")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[
                          {"source-id":"10","dc:title":"Revista de Investigaciones Veterinarias del Perú"},
                          {"source-id":"13","dc:title":"Ciencias Veterinarias"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas(
                "investigacones veterinarias", 25);

        assertThat(resultado.get(0).getTitulo())
                .isEqualTo("Revista de Investigaciones Veterinarias del Perú");
        server.verify();
    }

    @Test
    void rechazaCantidadesFueraDelLimiteDeScopus() {
        assertThatThrownBy(() -> service.buscarRevistas("medicine", 201))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void filtraPorLasAreasAsjcConfiguradasParaLaFacultad() {
        when(facultadRepository.findSubareaCodesByFacultad("FCAF"))
                .thenReturn(List.of("1102", "2300"));

        server.expect(requestTo(containsString("subjCode=1102%2C2300")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[{
                          "source-id":"1", "dc:title":"Agronomy Journal",
                          "prism:issn":"0001-0001",
                          "subject-area":[{"@code":"1102","@abbrev":"AGRI","$":"Agronomy and Crop Science"}]
                        },{
                          "source-id":"99", "dc:title":"Unrelated Chemistry Journal",
                          "prism:issn":"0099-0099",
                          "subject-area":[{"@code":"1602","@abbrev":"CHEM","$":"Analytical Chemistry"}]
                        },{
                          "source-id":"2", "dc:title":"Environmental Journal",
                          "prism:issn":"0002-0002",
                          "subject-area":[{"@code":"2300","@abbrev":"ENVI","$":"Environmental Science"}]
                        }]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas("science", 25, "FCAF");

        assertThat(resultado).extracting(RevistaDTO::getTitulo)
                .containsExactly("Agronomy Journal", "Environmental Journal");
        server.verify();
    }

    @Test
    void listaCamposAsjcGeneralesAsociadosALaFacultad() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1000", "1500", "1501", "1700", "1702"));

        server.expect(requestTo(containsString("subjCode=1500%2C1700")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[{
                          "subject-area":[
                            {"@code":"1702","@abbrev":"COMP","$":"Artificial Intelligence"},
                            {"@code":"1501","@abbrev":"CENG","$":"Bioengineering"},
                            {"@code":"1702","@abbrev":"COMP","$":"Artificial Intelligence"}
                          ]
                        }]}}
                        """, MediaType.APPLICATION_JSON));

        List<SubjectAreaDTO> resultado =
                service.obtenerCamposEstudioFacultad("FCI");

        assertThat(resultado)
                .extracting(SubjectAreaDTO::getCodigo)
                .containsExactly("1702", "1501");
        assertThat(resultado)
                .extracting(SubjectAreaDTO::getNombre)
                .containsExactly("Artificial Intelligence", "Bioengineering");
        server.verify();
    }

    @Test
    void filtraPorCampoAsjcCuandoFacultadYCampoEstanSeleccionados() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1000", "1500", "1501", "1700", "1702"));

        server.expect(requestTo(containsString("subjCode=1702")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[{
                          "source-id":"17", "dc:title":"Computing Journal",
                          "prism:issn":"0017-0017",
                          "subject-area":[{"@code":"1702","@abbrev":"COMP","$":"Artificial Intelligence"}]
                        }]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas(
                "computing", 25, "FCI", "1702");

        assertThat(resultado).extracting(RevistaDTO::getTitulo)
                .containsExactly("Computing Journal");
        server.verify();
    }

    @Test
    void rechazaCampoAsjcQueNoPerteneceALaFacultad() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1000", "1700", "1702"));

        assertThatThrownBy(() -> service.buscarRevistas(
                "medicine", 25, "FCI", "2700"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void cargaRevistasPorCampoSinTerminoDeBusqueda() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1700", "1702"));

        server.expect(requestTo(containsString("count=200")))
                .andExpect(requestTo(containsString("subjCode=1702")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[{
                          "source-id":"17", "dc:title":"AI Journal",
                          "prism:issn":"0017-0017",
                          "subject-area":[{"@code":"1702","@abbrev":"COMP","$":"Artificial Intelligence"}]
                        }]}}
                        """, MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas(
                "", 200, "FCI", "1702");

        assertThat(resultado).extracting(RevistaDTO::getTitulo)
                .containsExactly("AI Journal");
        server.verify();
    }

    @Test
    void paginaCampoUsaStartCountDeduplicaYReutilizaCache() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1700", "1702"));

        server.expect(requestTo(containsString("start=0")))
                .andExpect(requestTo(containsString("count=25")))
                .andExpect(requestTo(containsString("subjCode=1702")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{
                          "link":[
                            {"@ref":"next","@href":"https://api.example/title?start=25&count=25"},
                            {"@ref":"last","@href":"https://api.example/title?start=1267&count=25"}
                          ],
                          "entry":[{
                            "source-id":"17", "dc:title":"AI Journal",
                            "prism:issn":"0017-0017",
                            "subject-area":[{"@code":"1702","$":"Artificial Intelligence"}]
                          },{
                            "source-id":"999", "dc:title":"AI Journal duplicate",
                            "prism:issn":"0017-0017",
                            "subject-area":[{"@code":"1702","$":"Artificial Intelligence"}]
                          }]}}
                        """, MediaType.APPLICATION_JSON));

        RevistaPaginaDTO primera = service.buscarPaginaCampo("FCI", "1702", 0);
        RevistaPaginaDTO cache = service.buscarPaginaCampo("FCI", "1702", 0);

        assertThat(primera.contenido()).hasSize(1);
        RevistaDTO basica = primera.contenido().get(0);
        assertThat(basica.getSourceId()).isEqualTo("17");
        assertThat(basica.getTitulo()).isEqualTo("AI Journal");
        assertThat(basica.getIssn()).isEqualTo("0017-0017");
        assertThat(basica.getScopus().getSubjectAreas()).hasSize(1);
        assertThat(basica.getScopus().getCiteScore()).isNull();
        assertThat(basica.getScimago()).isNull();
        assertThat(basica.getProveedores()).isNull();
        assertThat(primera.totalResults()).isEqualTo(1292);
        assertThat(primera.totalEstimado()).isTrue();
        assertThat(primera.hasNext()).isTrue();
        assertThat(primera.paginaActual()).isEqualTo(1);
        assertThat(primera.totalPaginas()).isEqualTo(52);
        assertThat(cache).isSameAs(primera);
        verify(serialTitleService, never()).obtenerMetricas(anyString());
        verify(scimagoRepository, times(1)).findAllByIssnNormalizadoPattern("00170017");
        verify(springerRevistaService, never()).buscarParaBusqueda(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class));
        server.verify();
    }

    @Test
    void paginaCampoCargaLosCuatroFiltrosEnConsultasAgrupadas() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1702"));
        ScopusFuente fuente = new ScopusFuente();
        fuente.setIssn("0017-0017");
        fuente.setIssnNormalizado("00170017");
        fuente.setEstado("Active");
        fuente.setDiscontinuada(false);
        SerialTitleDTO metricasPagina = new SerialTitleDTO();
        metricasPagina.setQuartile("Q1");
        metricasPagina.setBestQuartile("Q1");
        when(serialTitleService.extraerMetricas(
                org.mockito.ArgumentMatchers.any())).thenReturn(metricasPagina);
        when(scopusFuenteRepository.findAllByIssnNormalizadoInOrEissnNormalizadoIn(
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(fuente));

        server.expect(requestTo(containsString("start=0")))
                .andExpect(requestTo(containsString("view=CITESCORE")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"opensearch:totalResults":"1","entry":[{
                          "source-id":"17", "dc:title":"AI Journal", "prism:issn":"0017-0017",
                          "openaccess":"1", "subject-area":[{"@code":"1702","$":"AI"}],
                          "citeScoreYearInfoList":{"citeScoreCurrentMetricYear":"2025",
                            "citeScoreYearInfo":[{"@year":"2025","citeScoreInformationList":[
                              {"citeScoreInfo":[{"citeScoreSubjectRank":[{"percentile":"88"}]}]}]}]}
                        }]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("start=25")))
                .andExpect(requestTo(containsString("view=CITESCORE")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"opensearch:totalResults":"26","entry":[{
                          "source-id":"17", "dc:title":"AI Journal page 2",
                          "prism:issn":"0017-0017", "openaccess":"1",
                          "subject-area":[{"@code":"1702","$":"AI"}]
                        }]}}
                        """, MediaType.APPLICATION_JSON));

        RevistaPaginaDTO pagina = service.buscarPaginaCampo("FCI", "1702", 0);
        RevistaPaginaDTO paginaSiguiente = service.buscarPaginaCampo("FCI", "1702", 25);
        RevistaPaginaDTO paginaAlVolver = service.buscarPaginaCampo("FCI", "1702", 0);
        RevistaDTO revista = pagina.contenido().get(0);
        RevistaDTO revistaSiguiente = paginaSiguiente.contenido().get(0);

        assertThat(revista.getCuartil()).isEqualTo("Q1");
        assertThat(revista.getScopus().getCuartil()).isEqualTo("Q1");
        assertThat(revista.getScopus().getAccesoAbierto()).isTrue();
        assertThat(revista.getProveedores().get("scopus_excel").getDatosAdicionales())
                .containsEntry("estado", "Active")
                .containsEntry("discontinuada", "No");
        assertThat(revistaSiguiente.getScopus().getAccesoAbierto()).isTrue();
        assertThat(revistaSiguiente.getProveedores().get("scopus_excel").getDatosAdicionales())
                .containsEntry("estado", "Active")
                .containsEntry("discontinuada", "No");
        assertThat(paginaAlVolver).isSameAs(pagina);
        verify(scopusFuenteRepository, times(2))
                .findAllByIssnNormalizadoInOrEissnNormalizadoIn(
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.anyCollection());
        verify(serialTitleService, never()).obtenerMetricas(anyString());
        server.verify();
    }

    @Test
    void detalleCargaMetricasSoloAlSolicitarloYReutilizaCache() {
        RevistaDTO primero = service.obtenerDetalleComplementario(
                "17", "0017-0017", null, "Editorial");
        RevistaDTO segundo = service.obtenerDetalleComplementario(
                "17", "0017-0017", null, "Editorial");

        assertThat(primero.getScopus().getCiteScore()).isNull();
        assertThat(primero.getScopus().getCuartil()).isEqualTo("Q1");
        assertThat(segundo).isSameAs(primero);
        verify(serialTitleService, times(1)).obtenerMetricas("0017-0017");
        verify(scimagoRepository, times(1)).findAllByIssnNormalizadoPattern("00170017");
        verify(springerRevistaService, times(1)).buscarParaBusqueda(
                null, "0017-0017", null);
    }

    @Test
    void paginaCampoPermiteLlegarAlUltimoBloque() {
        when(facultadRepository.findSubareaCodesByFacultad("FCI"))
                .thenReturn(List.of("1700", "1702"));

        server.expect(requestTo(containsString("start=1275")))
                .andExpect(requestTo(containsString("count=25")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{
                          "link":[
                            {"@ref":"prev","@href":"https://api.example/title?start=1250&count=25"},
                            {"@ref":"last","@href":"https://api.example/title?start=1267&count=25"}
                          ],
                          "entry":[{
                            "source-id":"1292", "dc:title":"Last Journal",
                            "prism:eIssn":"9999-9999",
                            "subject-area":[{"@code":"1702","$":"Artificial Intelligence"}]
                          }]}}
                        """, MediaType.APPLICATION_JSON));

        RevistaPaginaDTO ultima = service.buscarPaginaCampo("FCI", "1702", 1275);

        assertThat(ultima.contenido()).extracting(RevistaDTO::getTitulo)
                .containsExactly("Last Journal");
        assertThat(ultima.totalResults()).isEqualTo(1292);
        assertThat(ultima.paginaActual()).isEqualTo(52);
        assertThat(ultima.totalPaginas()).isEqualTo(52);
        assertThat(ultima.hasNext()).isFalse();
        server.verify();
    }

    @Test
    void listaFacultadesDesdeElRepositorioExistente() {
        when(facultadRepository.findAllFacultades()).thenReturn(List.<Object[]>of(
                new Object[]{"FCI", "Ciencias de la Ingeniería"},
                new Object[]{"FCS", "Ciencias de la Salud"}
        ));

        assertThat(service.obtenerFacultades())
                .extracting(facultad -> facultad.get("codigo"))
                .containsExactly("FCI", "FCS");
    }

    @Test
    void coincidenciaPorIssnUsaPaisYCuartilScimagoSinCalcular() {
        Scimago scimago = scimago(10D, "Food Chemistry", "03088146, 18737072",
                "United Kingdom", "Q2", "1976-2026");
        when(scimagoRepository.findAllByIssnNormalizadoPattern("03088146"))
                .thenReturn(List.of(scimago));
        responderScopus("0308-8146", "1873-7072", "Food Chemistry");

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getPais()).isEqualTo("United Kingdom");
        assertThat(revista.getCuartil()).isEqualTo("Q2");
        assertThat(revista.getOrigenCuartil()).isEqualTo("SCIMAGO");
        assertThat(revista.isEncontradaEnScimago()).isTrue();
        assertThat(revista.getScimago().getPais()).isEqualTo("United Kingdom");
        assertThat(revista.getScimago().getPublisher()).isNull();
        assertThat(revista.getScopus().getCiteScore()).isEqualTo("8.5");
        assertThat(revista.getScopus().getCuartil()).isNull();
        verify(serialTitleService, never()).obtenerMetricas(anyString());
        verify(serialTitleService).obtenerMetricasSinCalcularCuartil("0308-8146");
    }

    @Test
    void coincidenciaPorEissnSeUsaCuandoIssnImpresoNoCoincide() {
        Scimago scimago = scimago(11D, "Electronic Journal", "22223333",
                "Ecuador", "Q3", "2020-2026");
        when(scimagoRepository.findAllByIssnNormalizadoPattern("11112222"))
                .thenReturn(List.of());
        when(scimagoRepository.findAllByIssnNormalizadoPattern("22223333"))
                .thenReturn(List.of(scimago));
        responderScopus("1111-2222", "2222-3333", "Electronic Journal");

        RevistaDTO revista = service.buscarRevistas("1111-2222", 5).get(0);

        assertThat(revista.getCuartil()).isEqualTo("Q3");
        assertThat(revista.getPais()).isEqualTo("Ecuador");
        assertThat(revista.getOrigenCuartil()).isEqualTo("SCIMAGO");
        verify(serialTitleService, never()).obtenerMetricas(anyString());
    }

    @Test
    void reconoceIssnConGuionesEspaciosYFormatoCompacto() {
        Scimago scimago = scimago(12D, "Normalized Journal", "12345678",
                "Spain", "Q1", "2010-2026");
        when(scimagoRepository.findAllByIssnNormalizadoPattern("12345678"))
                .thenReturn(List.of(scimago));
        responderScopus(" 1234-5678 ", null, "Normalized Journal");

        RevistaDTO revista = service.buscarRevistas("1234-5678", 5).get(0);

        assertThat(revista.getCuartil()).isEqualTo("Q1");
        assertThat(revista.getOrigenCuartil()).isEqualTo("SCIMAGO");
        verify(scimagoRepository).findAllByIssnNormalizadoPattern("12345678");
    }

    @Test
    void ausenciaEnScimagoConservaCalculoActualDeScopus() {
        responderScopus("0308-8146", null, "Calculated Journal");

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getCuartil()).isEqualTo("Q1");
        assertThat(revista.getOrigenCuartil()).isEqualTo("SCOPUS_CALCULADO");
        assertThat(revista.isEncontradaEnScimago()).isFalse();
        verify(serialTitleService).obtenerMetricas("0308-8146");
    }

    @Test
    void sinScimagoNiPercentilNoInventaCuartil() {
        when(serialTitleService.obtenerMetricas(anyString())).thenReturn(null);
        responderScopus("0308-8146", null, "Journal Without Metrics");

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getCuartil()).isNull();
        assertThat(revista.getOrigenCuartil()).isEqualTo("NO_DISPONIBLE");
        assertThat(revista.isEncontradaEnScimago()).isFalse();
    }

    @Test
    void coincidenciaScopusScimagoProduceUnaSolaRevista() {
        Scimago scimago = scimago(13D, "Unique Journal", "03088146",
                "Chile", "Q4", "2015-2026");
        when(scimagoRepository.findAllByIssnNormalizadoPattern("03088146"))
                .thenReturn(List.of(scimago));
        responderScopus("0308-8146", null, "Unique Journal");

        List<RevistaDTO> resultado = service.buscarRevistas("0308-8146", 5);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getScopus()).isNotNull();
        assertThat(resultado.get(0).getCuartil()).isEqualTo("Q4");
    }

    @Test
    void registroSoloScimagoNoSeAgregaAlResultado() {
        when(scimagoRepository.findAllByIssnNormalizadoPattern("99998888"))
                .thenReturn(List.of(scimago(14D, "Only SCImago", "99998888",
                        "Peru", "Q2", "2022-2026")));
        server.expect(requestTo(containsString("issn=99998888")))
                .andRespond(withSuccess(
                        "{\"serial-metadata-response\":{\"entry\":[]}}",
                        MediaType.APPLICATION_JSON));

        List<RevistaDTO> resultado = service.buscarRevistas("9999-8888", 5);

        assertThat(resultado).isEmpty();
        verify(scimagoRepository, never()).findAllByIssnNormalizadoPattern(anyString());
    }

    @Test
    void multiplesRegistrosEligenAnioMasRecienteYLuegoMejorCuartil() {
        Scimago antiguoQ1 = scimago(20D, "Multi Journal", "03088146",
                "Old Country", "Q1", "2000-2024");
        Scimago recienteQ3 = scimago(21D, "Multi Journal", "03088146",
                "Country Q3", "Q3", "2000-2026");
        Scimago recienteQ1 = scimago(22D, "Multi Journal", "03088146",
                "Country Q1", "Q1", "2000-2026");
        when(scimagoRepository.findAllByIssnNormalizadoPattern("03088146"))
                .thenReturn(List.of(antiguoQ1, recienteQ3, recienteQ1));
        responderScopus("0308-8146", null, "Multi Journal");

        RevistaDTO revista = service.buscarRevistas("0308-8146", 5).get(0);

        assertThat(revista.getPais()).isEqualTo("Country Q1");
        assertThat(revista.getCuartil()).isEqualTo("Q1");
        assertThat(revista.getOrigenCuartil()).isEqualTo("SCIMAGO");
        verify(serialTitleService, never()).obtenerMetricas(anyString());
    }

    private void responderScopus(String issn, String eIssn, String titulo) {
        String eIssnJson = eIssn == null ? "" : ", \"prism:eIssn\":\"" + eIssn + "\"";
        server.expect(requestTo(containsString("issn=")))
                .andRespond(withSuccess("""
                        {"serial-metadata-response":{"entry":[{
                          "source-id":"24039", "dc:title":"%s",
                          "prism:issn":"%s"%s
                        }]}}
                        """.formatted(titulo, issn, eIssnJson), MediaType.APPLICATION_JSON));
    }

    private Scimago scimago(
            Double sourceId, String titulo, String issn, String pais,
            String cuartil, String cobertura
    ) {
        Scimago scimago = new Scimago();
        scimago.setSourceid(sourceId);
        scimago.setTitle(titulo);
        scimago.setIssn(issn);
        scimago.setCountry(pais);
        scimago.setSJRBestQuartile(cuartil);
        scimago.setCoverage(cobertura);
        return scimago;
    }

}
