package uteq.edu.ec.sicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.sicrec.dto.SerialTitleDTO;
@Service
public class SerialTitleService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${scopus.api.key}")
    private String apiKey;

    @Value("${scopus.serial.url}")
    private String serialUrl;

    public SerialTitleService(RestClient restClient) {

        this.restClient = restClient;

    }
    public SerialTitleDTO obtenerMetricas(String issn) {

        try {

            String respuesta = restClient
                    .get()
                    .uri(serialUrl + "?issn=" + issn + "&view=CITESCORE")
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(respuesta);

            JsonNode entry = root
                    .path("serial-metadata-response")
                    .path("entry");

            if (!entry.isArray() || entry.isEmpty()) {
                return null;
            }

            JsonNode revista = entry.get(0);

            SerialTitleDTO dto = new SerialTitleDTO();

            dto.setPublisher(
                    obtenerTexto(revista, "dc:publisher")
            );

// ===================
// SJR
// ===================

            JsonNode sjr = revista.path("SJRList")
                    .path("SJR")
                    .get(0);

            if (sjr != null) {

                dto.setSjr(sjr.path("$").asText());
                dto.setSjrYear(sjr.path("@year").asText());

            }

// ===================
// SNIP
// ===================

            JsonNode snip = revista.path("SNIPList")
                    .path("SNIP")
                    .get(0);

            if (snip != null) {

                dto.setSnip(snip.path("$").asText());
                dto.setSnipYear(snip.path("@year").asText());

            }

// ===================
// CiteScore
// ===================

            dto.setCiteScore(

                    revista.path("citeScoreYearInfoList")
                            .path("citeScoreCurrentMetric")
                            .asText()

            );

            dto.setCiteScoreYear(

                    revista.path("citeScoreYearInfoList")
                            .path("citeScoreCurrentMetricYear")
                            .asText()

            );
            try {

                JsonNode percentile = revista
                        .path("citeScoreYearInfoList")
                        .path("citeScoreInformationList")
                        .get(0)
                        .path("citeScoreInfo")
                        .get(0)
                        .path("citeScoreSubjectRank")
                        .get(0)
                        .path("percentile");

                if (!percentile.isMissingNode()) {

                    dto.setPercentile(percentile.asText());

                    int p = Integer.parseInt(percentile.asText());

                    if (p >= 75)
                        dto.setQuartile("Q1");
                    else if (p >= 50)
                        dto.setQuartile("Q2");
                    else if (p >= 25)
                        dto.setQuartile("Q3");
                    else
                        dto.setQuartile("Q4");

                }

            } catch (Exception ignored) {

            }
            return dto;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }
    private String obtenerTexto(JsonNode nodo, String campo) {

        JsonNode valor = nodo.get(campo);

        if (valor == null || valor.isNull()) {
            return null;
        }

        return valor.asText();

    }

}