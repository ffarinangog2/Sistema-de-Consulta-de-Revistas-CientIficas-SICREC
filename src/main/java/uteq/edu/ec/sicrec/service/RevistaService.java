package uteq.edu.ec.sicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.sicrec.dto.RevistaDTO;
import uteq.edu.ec.sicrec.dto.ScimagoInfoDTO;
import uteq.edu.ec.sicrec.dto.ScopusInfoDTO;
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

    @Value("${scopus.serial.url}")
    private String serialUrl;

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

        String terminoLimpio = termino == null ? "" : termino.trim();
        boolean esIssn = esFormatoIssn(terminoLimpio);

        try {

            String parametroBusqueda = esIssn
                    ? "issn=" + normalizarIssn(terminoLimpio)
                    : "title=" + terminoLimpio;

            String respuestaScopus = restClient
                    .get()
                    .uri(serialUrl + "?" + parametroBusqueda + "&count=" + cantidad + "&view=STANDARD")
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(respuestaScopus);

            JsonNode entries = root
                    .path("serial-metadata-response")
                    .path("entry");

            boolean scopusTuvoResultados = entries.isArray() && entries.size() > 0;

            if (scopusTuvoResultados) {

                for (JsonNode entry : entries) {
                    revistas.add(construirDesdeScopus(entry));
                }

            } else if (esIssn) {

                // No existe en Scopus, pero puede existir en SCImago
                RevistaDTO soloScimago = construirSoloDesdeScimago(normalizarIssn(terminoLimpio));

                if (soloScimago != null) {
                    revistas.add(soloScimago);
                }

            }

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

        return revistas;

    }

    // ===============================
    // Construcción a partir de un entry de Scopus
    // ===============================
    private RevistaDTO construirDesdeScopus(JsonNode entry) {

        String titulo = obtenerTexto(entry, "dc:title");
        String revistaNombre = titulo;
        String issn = obtenerTexto(entry, "prism:issn");
        String eIssn = obtenerTexto(entry, "prism:eIssn");
        String sourceId = obtenerTexto(entry, "source-id");
        String fecha = obtenerTexto(entry, "prism:coverDate");

        Boolean accesoAbierto = entry.path("openaccessFlag").asBoolean(false);
        String enlaceScopus = obtenerEnlaceScopus(entry);
        String pais = obtenerPais(entry);

        // Identificador para CONSULTAR MÉTRICAS únicamente (no para mostrar):
        // prioridad ISSN -> eISSN. Esto solo decide qué identificador se manda
        // a SerialTitleService para poder obtener CiteScore/SJR/SNIP/Cuartil
        // cuando la revista solo tiene eISSN. No afecta lo que se muestra.
        String identificadorMetricas =
                (issn != null && !issn.isBlank()) ? issn : eIssn;

        SerialTitleDTO metricas = null;

        if (identificadorMetricas != null && !identificadorMetricas.isBlank()) {
            metricas = serialTitleService.obtenerMetricas(identificadorMetricas);
        }

        ScopusInfoDTO scopusInfo = new ScopusInfoDTO();
        scopusInfo.setEncontrado(true);
        scopusInfo.setPais(pais);
        scopusInfo.setAccesoAbierto(accesoAbierto);
        scopusInfo.setEnlaceScopus(enlaceScopus);

        String cuartilScopus = null;

        if (metricas != null) {

            scopusInfo.setPublisher(metricas.getPublisher());

            scopusInfo.setSjr(metricas.getSjr());
            scopusInfo.setSjrYear(metricas.getSjrYear());

            scopusInfo.setSnip(metricas.getSnip());
            scopusInfo.setSnipYear(metricas.getSnipYear());

            scopusInfo.setCiteScore(metricas.getCiteScore());
            scopusInfo.setCiteScoreYear(metricas.getCiteScoreYear());

            scopusInfo.setPercentile(metricas.getPercentile());

            cuartilScopus = metricas.getQuartile();
            scopusInfo.setCuartil(cuartilScopus);

        }

        Optional<Scimago> scimago = buscarScimago(sourceId, issn, eIssn);

        ScimagoInfoDTO scimagoInfo = scimago
                .map(this::mapearScimago)
                .orElse(null);

        RevistaDTO dto = new RevistaDTO();
        dto.setTitulo(titulo);
        dto.setRevista(revistaNombre);

        // ISSN y eISSN totalmente independientes: cada uno se envía tal cual
        // viene de Scopus, sin usar uno como respaldo del otro.
        dto.setIssn(issn);
        dto.setEIssn(eIssn);

        dto.setSourceId(sourceId);
        dto.setFecha(fecha);

        dto.setCuartil(
                cuartilScopus != null
                        ? cuartilScopus
                        : (scimagoInfo != null ? scimagoInfo.getCuartil() : null)
        );

        dto.setScopus(scopusInfo);
        dto.setScimago(scimagoInfo);

        return dto;

    }

    // ===============================
    // Fallback: la revista solo existe en SCImago
    // (búsqueda por ISSN sin resultados en Scopus)
    // ===============================
    private RevistaDTO construirSoloDesdeScimago(String issn) {

        Optional<Scimago> scimago = scimagoRepository.findByIssnContaining(issn);

        if (scimago.isEmpty()) {
            return null;
        }

        ScimagoInfoDTO scimagoInfo = mapearScimago(scimago.get());

        RevistaDTO dto = new RevistaDTO();
        dto.setTitulo(scimagoInfo.getTitulo());
        dto.setRevista(scimagoInfo.getTitulo());
        dto.setIssn(issn);
        dto.setEIssn(null);
        dto.setSourceId(
                scimago.get().getSourceid() != null
                        ? String.valueOf(scimago.get().getSourceid().longValue())
                        : null
        );
        dto.setFecha(null);
        dto.setCuartil(scimagoInfo.getCuartil());

        dto.setScopus(null);
        dto.setScimago(scimagoInfo);

        return dto;

    }

    private Optional<Scimago> buscarScimago(String sourceId, String issn, String eIssn) {

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

        return scimago;

    }

    private ScimagoInfoDTO mapearScimago(Scimago s) {

        ScimagoInfoDTO info = new ScimagoInfoDTO();
        info.setEncontrado(true);
        info.setTitulo(s.getTitle());
        info.setIssn(s.getIssn());
        info.setPublisher(
                s.getPublisher23() != null ? s.getPublisher23() : s.getPublisher6()
        );
        info.setPais(s.getCountry());
        info.setRegion(s.getRegion());
        info.setSjr(s.getSjr());
        info.setCuartil(s.getSJRBestQuartile());
        info.setHIndex(
                s.getHIndex() == null ? null : String.valueOf(s.getHIndex().longValue())
        );
        info.setCobertura(s.getCoverage());
        info.setCategorias(s.getCategories());
        info.setAreas(s.getAreas());
        info.setRank(
                s.getRank() == null ? null : String.valueOf(s.getRank().longValue())
        );
        info.setOpenAccess(s.getOpenAccess());

        return info;

    }

    // ===============================
    // Detección de ISSN / eISSN
    // ===============================
    private boolean esFormatoIssn(String texto) {

        if (texto == null || texto.isBlank()) {
            return false;
        }

        return texto.trim().toUpperCase().matches("\\d{4}-?\\d{3}[\\dX]");

    }

    private String normalizarIssn(String texto) {

        return texto.trim().toUpperCase().replace("-", "");

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