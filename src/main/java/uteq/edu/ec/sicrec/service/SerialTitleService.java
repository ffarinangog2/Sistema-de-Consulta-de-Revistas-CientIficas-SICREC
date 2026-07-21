package uteq.edu.ec.sicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.sicrec.dto.SerialTitleDTO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

            JsonNode citeScoreYearInfoList = revista.path("citeScoreYearInfoList");

            dto.setCiteScore(
                    citeScoreYearInfoList
                            .path("citeScoreCurrentMetric")
                            .asText()
            );

            String anioVigente = citeScoreYearInfoList
                    .path("citeScoreCurrentMetricYear")
                    .asText();

            dto.setCiteScoreYear(anioVigente);

// ===================
// Percentil / Cuartil
// Se usa ÚNICAMENTE el percentile del año vigente (citeScoreCurrentMetricYear),
// no el máximo histórico. Se localiza primero el objeto de citeScoreYearInfo
// cuyo "@year" coincide con ese año, y solo dentro de ese objeto se busca
// el percentile (de forma recursiva por si varía la profundidad de anidación,
// pero SIN salir del año vigente).
// ===================

            JsonNode nodoAnioVigente = buscarNodoDeAnio(
                    citeScoreYearInfoList.path("citeScoreYearInfo"),
                    anioVigente
            );

            List<Integer> percentiles = new ArrayList<>();

            if (nodoAnioVigente != null) {
                buscarPercentilesRecursivo(nodoAnioVigente, percentiles);
            }

            System.out.println(
                    "DEBUG [" + issn + "] año vigente: " + anioVigente
                            + " | percentiles encontrados en ese año: " + percentiles
            );

            if (!percentiles.isEmpty()) {

                // Si hay varias categorías/áreas dentro del mismo año vigente,
                // se toma el mejor percentil de ese año (no de todo el historial).
                int mejorPercentil = percentiles
                        .stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElseThrow();

                dto.setPercentile(String.valueOf(mejorPercentil));
                dto.setBestPercentile(String.valueOf(mejorPercentil));

                String cuartil;

                if (mejorPercentil >= 75)
                    cuartil = "Q1";
                else if (mejorPercentil >= 50)
                    cuartil = "Q2";
                else if (mejorPercentil >= 25)
                    cuartil = "Q3";
                else
                    cuartil = "Q4";

                dto.setQuartile(cuartil);
                dto.setBestQuartile(cuartil);

                System.out.println(
                        "DEBUG [" + issn + "] mejor percentil del año vigente: " + mejorPercentil
                                + " -> cuartil calculado: " + cuartil
                );

            } else {

                System.out.println(
                        "DEBUG [" + issn + "] no se encontró percentile para el año vigente (" + anioVigente + ")."
                );

            }

            return dto;

        } catch (Exception e) {

            // Una revista sin métricas disponibles no debe interrumpir toda la búsqueda.
            return null;

        }

    }

    // ===============================
    // Busca, dentro de un arreglo citeScoreYearInfo, el objeto cuyo "@year"
    // coincide con el año vigente. Devuelve null si no se encuentra.
    // ===============================
    private JsonNode buscarNodoDeAnio(JsonNode citeScoreYearInfo, String anioVigente) {

        if (citeScoreYearInfo == null || !citeScoreYearInfo.isArray() || anioVigente == null) {
            return null;
        }

        for (JsonNode anioNodo : citeScoreYearInfo) {

            String anioNodoTexto = anioNodo.path("@year").asText();

            if (anioVigente.equals(anioNodoTexto)) {
                return anioNodo;
            }

        }

        return null;

    }

    // ===============================
    // Recorre recursivamente cualquier estructura JSON (objetos y arreglos)
    // buscando campos llamados "percentile" y acumulando sus valores numéricos.
    // Se invoca acotado a un único nodo de año, para no mezclar percentiles
    // de otros años.
    // ===============================
    private void buscarPercentilesRecursivo(JsonNode nodo, List<Integer> acumulador) {

        if (nodo == null || nodo.isMissingNode() || nodo.isNull()) {
            return;
        }

        if (nodo.isObject()) {

            Iterator<Map.Entry<String, JsonNode>> campos = nodo.fields();

            while (campos.hasNext()) {

                Map.Entry<String, JsonNode> campo = campos.next();

                if ("percentile".equalsIgnoreCase(campo.getKey())) {

                    Integer valor = parsearEntero(campo.getValue());

                    if (valor != null) {
                        acumulador.add(valor);
                    }

                } else {

                    buscarPercentilesRecursivo(campo.getValue(), acumulador);

                }

            }

        } else if (nodo.isArray()) {

            for (JsonNode elemento : nodo) {
                buscarPercentilesRecursivo(elemento, acumulador);
            }

        }

    }

    private Integer parsearEntero(JsonNode nodo) {

        try {

            String texto = nodo.isTextual() ? nodo.asText() : nodo.asText();

            if (texto == null || texto.isBlank()) {
                return null;
            }

            return Integer.parseInt(texto.trim());

        } catch (Exception e) {

            return null;

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
