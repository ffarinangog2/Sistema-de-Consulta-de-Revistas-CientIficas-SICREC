package uteq.edu.ec.sicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.sicrec.dto.RevistaDTO;
import uteq.edu.ec.sicrec.dto.SerialTitleDTO;
import uteq.edu.ec.sicrec.entity.Scimago;
import uteq.edu.ec.sicrec.repository.ScimagoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RevistaService {

    private final RestClient restClient;
    private final ScimagoRepository scimagoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SerialTitleService serialTitleService;

    @Value("${scopus.api.key}")
    private String apiKey;

    @Value("${scopus.base.url}")
    private String baseUrl;

    public RevistaService(
            RestClient restClient,
            ScimagoRepository scimagoRepository,
            SerialTitleService serialTitleService
    ) {
        this.restClient = restClient;
        this.scimagoRepository = scimagoRepository;
        this.serialTitleService = serialTitleService;
    }

    public List<RevistaDTO> buscarRevistas(String termino, Integer cantidad) {

        List<RevistaDTO> revistas = new ArrayList<>();

        try {

            String respuestaScopus = restClient
                    .get()
                    .uri(baseUrl + "?query=" + termino + "&count=" + cantidad)
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(respuestaScopus);

            JsonNode entries = root
                    .path("search-results")
                    .path("entry");

            if (!entries.isArray()) {
                return revistas;
            }

            for (JsonNode entry : entries) {

                String titulo = obtenerTexto(entry, "dc:title");
                String revistaNombre = obtenerTexto(entry, "prism:publicationName");
                String issn = obtenerTexto(entry, "prism:issn");
                String eIssn = obtenerTexto(entry, "prism:eIssn");
                String sourceId = obtenerTexto(entry, "source-id");
                String fecha = obtenerTexto(entry, "prism:coverDate");

                Boolean accesoAbierto = entry.path("openaccessFlag").asBoolean(false);

                String enlaceScopus = obtenerEnlaceScopus(entry);

                String pais = obtenerPais(entry);

                // ===============================
                // NUEVO: MÉTRICAS DESDE SCOPUS
                // ===============================

                SerialTitleDTO metricas = null;

                if (issn != null && !issn.isBlank()) {
                    metricas = serialTitleService.obtenerMetricas(issn);
                }

                // ===============================
                // RESPALDO SCIMAGO
                // ===============================

                Optional<Scimago> scimago = Optional.empty();

                if (sourceId != null && !sourceId.isBlank()) {

                    try {
                        scimago = scimagoRepository.findBySourceid(Double.valueOf(sourceId));
                    } catch (NumberFormatException ignored) {
                    }

                }

                if (scimago.isEmpty() && issn != null && !issn.isBlank()) {
                    scimago = scimagoRepository.findByIssnContaining(issn);
                }

                if (scimago.isEmpty() && eIssn != null && !eIssn.isBlank()) {
                    scimago = scimagoRepository.findByIssnContaining(eIssn);
                }

                RevistaDTO dto = new RevistaDTO();

                dto.setTitulo(titulo);
                dto.setRevista(revistaNombre);
                dto.setIssn(issn);
                dto.setEIssn(eIssn);
                dto.setSourceId(sourceId);
                dto.setFecha(fecha);
                dto.setPais(pais);
                dto.setAccesoAbierto(accesoAbierto);
                dto.setEnlaceScopus(enlaceScopus);

                // ===============================
                // DATOS OFICIALES DE SCOPUS
                // ===============================

                if (metricas != null) {

                    dto.setPublisher(metricas.getPublisher());

                    dto.setSjr(metricas.getSjr());
                    dto.setSjrYear(metricas.getSjrYear());

                    dto.setSnip(metricas.getSnip());
                    dto.setSnipYear(metricas.getSnipYear());

                    dto.setCiteScore(metricas.getCiteScore());
                    dto.setCiteScoreYear(metricas.getCiteScoreYear());

                    dto.setPercentile(metricas.getPercentile());

                    if (metricas.getQuartile() != null) {
                        dto.setCuartil(metricas.getQuartile());
                    }

                }                // ===============================
                // RESPALDO SCIMAGO
                // ===============================

                if (scimago.isPresent()) {

                    Scimago s = scimago.get();

                    // Solo usar el cuartil de SCImago si Scopus no lo obtuvo
                    if (dto.getCuartil() == null) {
                        dto.setCuartil(s.getSJRBestQuartile());
                    }

                    dto.setHIndex(
                            s.getHIndex() == null
                                    ? null
                                    : s.getHIndex().toString()
                    );

                    dto.setCobertura(s.getCoverage());
                    dto.setCategorias(s.getCategories());
                    dto.setAreas(s.getAreas());

                }

                revistas.add(dto);

            }

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

        return revistas;

    }

    private String obtenerTexto(JsonNode nodo, String campo) {

        JsonNode valor = nodo.get(campo);

        if (valor == null || valor.isNull()) {
            return null;
        }

        return valor.asText();

    }

    private String obtenerPais(JsonNode entry) {

        JsonNode affiliation = entry.path("affiliation");

        if (affiliation.isArray() && affiliation.size() > 0) {

            JsonNode primera = affiliation.get(0);

            return obtenerTexto(
                    primera,
                    "affiliation-country"
            );

        }

        return null;

    }

    private String obtenerEnlaceScopus(JsonNode entry) {

        JsonNode links = entry.path("link");

        if (links.isArray()) {

            for (JsonNode link : links) {

                String ref = obtenerTexto(
                        link,
                        "@ref"
                );

                if ("scopus".equals(ref)) {

                    return obtenerTexto(
                            link,
                            "@href"
                    );

                }

            }

        }

        return null;

    }

}