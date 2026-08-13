package uteq.edu.ec.crecuteq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.RevistaDTO;
import uteq.edu.ec.crecuteq.dto.RevistaPaginaDTO;
import uteq.edu.ec.crecuteq.dto.ScimagoInfoDTO;
import uteq.edu.ec.crecuteq.dto.ScopusInfoDTO;
import uteq.edu.ec.crecuteq.dto.SerialTitleDTO;
import uteq.edu.ec.crecuteq.dto.SubjectAreaDTO;
import uteq.edu.ec.crecuteq.dto.SpringerInfoDTO;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.entity.Scimago;
import uteq.edu.ec.crecuteq.entity.ScopusFuente;
import uteq.edu.ec.crecuteq.repository.ScimagoRepository;
import uteq.edu.ec.crecuteq.repository.ScopusFuenteRepository;
import uteq.edu.ec.crecuteq.repository.FacultadScopusAreaRepository;
import uteq.edu.ec.crecuteq.provider.JournalProviderRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

@Service
public class RevistaService {

    private static final int TAMANO_PAGINA_CAMPO = 25;
    private static final Pattern START_EN_ENLACE = Pattern.compile("(?:[?&])start=(\\d+)");
    private static final Pattern ANIO = Pattern.compile("(?:19|20)\\d{2}");
    private static final String CUARTIL_SCIMAGO = "SCIMAGO";
    private static final String CUARTIL_SCOPUS_CALCULADO = "SCOPUS_CALCULADO";
    private static final String CUARTIL_NO_DISPONIBLE = "NO_DISPONIBLE";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RevistaService.class);

    private final RestClient restClient;
    private final ScimagoRepository scimagoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SerialTitleService serialTitleService;
    private final SpringerRevistaService springerRevistaService;
    // INICIO - Registro Strategy de proveedores
    private final JournalProviderRegistry journalProviderRegistry;
    private FacultadScopusAreaRepository facultadScopusAreaRepository;
    private ScopusFuenteRepository scopusFuenteRepository;
    private final Map<PaginaCampoClave, PaginaCampoCache> paginasCampoCache =
            new ConcurrentHashMap<>();
    private final Map<String, DetalleCache> detallesCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RevistaDTO>> detallesEnCurso =
            new ConcurrentHashMap<>();
    // FIN - Registro Strategy de proveedores

    @Value("${scopus.api.key}")
    private String apiKey;

    @Value("${scopus.serial.url}")
    private String serialUrl;

    @Value("${scopus.pagination.cache-ttl-seconds:300}")
    private long cacheTtlSegundos;

    @Value("${scopus.detalle.cache-ttl-seconds:600}")
    private long detalleCacheTtlSegundos;

    public RevistaService(
            RestClient restClient,
            ScimagoRepository scimagoRepository,
            SerialTitleService serialTitleService,
            SpringerRevistaService springerRevistaService
    ) {
        this.restClient = restClient;
        this.scimagoRepository = scimagoRepository;
        this.serialTitleService = serialTitleService;
        this.springerRevistaService = springerRevistaService;
        this.journalProviderRegistry = null;
    }

    // INICIO - Inyección extensible de estrategias sin romper el constructor existente
    @Autowired
    public RevistaService(
            RestClient restClient,
            ScimagoRepository scimagoRepository,
            SerialTitleService serialTitleService,
            SpringerRevistaService springerRevistaService,
            JournalProviderRegistry journalProviderRegistry
    ) {
        this.restClient = restClient;
        this.scimagoRepository = scimagoRepository;
        this.serialTitleService = serialTitleService;
        this.springerRevistaService = springerRevistaService;
        this.journalProviderRegistry = journalProviderRegistry;
    }
    // FIN - Inyección extensible de estrategias sin romper el constructor existente

    @Autowired
    void configurarFacultades(FacultadScopusAreaRepository repository) {
        this.facultadScopusAreaRepository = repository;
    }

    @Autowired
    void configurarFuentesScopus(ScopusFuenteRepository repository) {
        this.scopusFuenteRepository = repository;
    }

    public List<RevistaDTO> buscarRevistas(String termino, Integer cantidad) {

        return buscarRevistas(termino, cantidad, null);
    }

    /**
     * Conserva la consulta original cuando facultad es nula. Con una facultad
     * configurada consulta cada código ASJC mediante el parámetro subjCode de
     * Serial Title API y elimina duplicados antes de enriquecer los resultados.
     */
    public List<RevistaDTO> buscarRevistas(String termino, Integer cantidad, String facultad) {

        return buscarRevistas(termino, cantidad, facultad, null);
    }

    public List<RevistaDTO> buscarRevistas(
            String termino, Integer cantidad, String facultad, String campoEstudio
    ) {

        List<RevistaDTO> revistas = new ArrayList<>();

        String terminoLimpio = termino == null ? "" : termino.trim();

        boolean cargaAutomatica = !terminoLimpio.isBlank()
                ? false
                : facultad != null && !facultad.isBlank()
                && campoEstudio != null && !campoEstudio.isBlank();

        if (terminoLimpio.isBlank() && !cargaAutomatica) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El término de búsqueda es obligatorio."
            );
        }

        if (cantidad == null || cantidad < 1 || cantidad > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe estar entre 1 y 200."
            );
        }

        boolean esIssn = esFormatoIssn(terminoLimpio);
        List<String> codigosAsjc = obtenerCodigosAsjcFacultad(facultad);
        codigosAsjc = filtrarCodigosPorCampo(codigosAsjc, facultad, campoEstudio);

        try {
            Map<String, JsonNode> entriesUnicas = new LinkedHashMap<>();
            if (codigosAsjc.isEmpty()) {
                if (esIssn) {
                    agregarEntries(entriesUnicas, consultarScopus(
                            terminoLimpio, cantidad, true, null), null);
                } else {
                    entriesUnicas.putAll(consultarTituloAproximado(
                            terminoLimpio, cantidad));
                }
            } else {
                Set<String> codigosPermitidos = new HashSet<>(codigosAsjc);
                String codigoConsulta = campoEstudio == null || campoEstudio.isBlank()
                        ? String.join(",", codigosAsjc)
                        : campoEstudio.trim();
                agregarEntries(entriesUnicas, consultarScopus(
                        terminoLimpio, cantidad, esIssn, codigoConsulta),
                        codigosPermitidos);
            }

            for (JsonNode entry : entriesUnicas.values()) {
                if (revistas.size() >= cantidad) break;
                revistas.add(construirDesdeScopus(entry));
            }

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar la API de Scopus.",
                    e
            );

        }

        return revistas;

    }

    public RevistaPaginaDTO buscarPaginaCampo(
            String facultad, String campoEstudio, Integer start
    ) {
        int inicio = start == null ? 0 : start;
        if (inicio < 0 || inicio % TAMANO_PAGINA_CAMPO != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "start debe ser cero o un múltiplo de 25.");
        }

        List<String> codigos = filtrarCodigosPorCampo(
                obtenerCodigosAsjcFacultad(facultad), facultad, campoEstudio);
        String codigo = codigos.get(0);
        PaginaCampoClave clave = new PaginaCampoClave(codigo, inicio, TAMANO_PAGINA_CAMPO);
        long ahora = System.currentTimeMillis();
        PaginaCampoCache almacenada = paginasCampoCache.get(clave);
        if (almacenada != null && ahora - almacenada.creadaEnMillis()
                < cacheTtlSegundos * 1_000L) {
            return almacenada.pagina();
        }

        try {
            JsonNode raiz = consultarScopusPaginado(
                    codigo, inicio, TAMANO_PAGINA_CAMPO);
            JsonNode entries = raiz.path("entry");
            Map<String, JsonNode> unicas = deduplicarPagina(entries, Set.of(codigo));
            Map<String, Scimago> scimagoPorIssn = cargarScimagoPagina(unicas.values());
            Map<String, ScopusFuente> fuentesPorIssn = cargarFuentesPagina(unicas.values());
            List<RevistaDTO> contenido = new ArrayList<>(unicas.size());
            for (JsonNode entry : unicas.values()) {
                contenido.add(construirBasicoDesdeScopus(
                        entry, scimagoPorIssn, fuentesPorIssn));
            }

            NavegacionScopus navegacion = obtenerNavegacion(
                    raiz, inicio, TAMANO_PAGINA_CAMPO, entries.size());
            RevistaPaginaDTO pagina = new RevistaPaginaDTO(
                    List.copyOf(contenido), inicio, TAMANO_PAGINA_CAMPO,
                    navegacion.totalResults(), navegacion.totalEstimado(),
                    navegacion.hasNext(), inicio / TAMANO_PAGINA_CAMPO + 1,
                    navegacion.totalResults() == 0 ? 0
                            : (int) Math.ceil((double) navegacion.totalResults()
                            / TAMANO_PAGINA_CAMPO)
            );
            paginasCampoCache.put(clave, new PaginaCampoCache(ahora, pagina));
            paginasCampoCache.entrySet().removeIf(entry ->
                    ahora - entry.getValue().creadaEnMillis()
                            >= cacheTtlSegundos * 1_000L);
            return pagina;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar la página de Scopus.", exception);
        }
    }

    /**
     * Reúne todas las páginas del campo para que los filtros y sus conteos se
     * calculen sobre el resultado completo, no únicamente sobre una página.
     */
    public List<RevistaDTO> buscarTodasCampo(String facultad, String campoEstudio) {
        List<RevistaDTO> resultado = new ArrayList<>();
        Set<String> identidades = new HashSet<>();
        int inicio = 0;

        while (true) {
            RevistaPaginaDTO pagina = buscarPaginaCampo(facultad, campoEstudio, inicio);
            for (RevistaDTO revista : pagina.contenido()) {
                String identidad = primerNoVacio(
                        normalizarIdentificador(revista.getSourceId()),
                        normalizarIdentificador(revista.getIssn()),
                        normalizarIdentificador(revista.getEIssn()),
                        normalizarTextoBusqueda(revista.getTitulo())
                );
                if (identidad == null || identidades.add(identidad)) {
                    resultado.add(revista);
                }
            }
            if (!pagina.hasNext()) break;
            inicio += TAMANO_PAGINA_CAMPO;
        }

        return List.copyOf(resultado);
    }

    public RevistaDTO obtenerDetalleComplementario(
            String sourceId, String issn, String eIssn, String editorial
    ) {
        String clave = claveDetalle(sourceId, issn, eIssn);
        long ahora = System.currentTimeMillis();
        DetalleCache almacenado = detallesCache.get(clave);
        if (almacenado != null && ahora - almacenado.creadoEnMillis()
                < detalleCacheTtlSegundos * 1_000L) {
            return almacenado.detalle();
        }

        CompletableFuture<RevistaDTO> nueva = new CompletableFuture<>();
        CompletableFuture<RevistaDTO> existente = detallesEnCurso.putIfAbsent(clave, nueva);
        if (existente != null) {
            try {
                return existente.join();
            } catch (CompletionException exception) {
                throw convertirErrorDetalle(exception.getCause());
            }
        }

        try {
            RevistaDTO detalle = construirDetalleComplementario(
                    sourceId, issn, eIssn, editorial);
            detallesCache.put(clave, new DetalleCache(ahora, detalle));
            nueva.complete(detalle);
            return detalle;
        } catch (RuntimeException exception) {
            nueva.completeExceptionally(exception);
            throw exception;
        } finally {
            detallesEnCurso.remove(clave, nueva);
            detallesCache.entrySet().removeIf(entry ->
                    ahora - entry.getValue().creadoEnMillis()
                            >= detalleCacheTtlSegundos * 1_000L);
        }
    }

    private RevistaDTO construirBasicoDesdeScopus(
            JsonNode entry, Map<String, Scimago> scimagoPorIssn,
            Map<String, ScopusFuente> fuentesPorIssn) {
        String titulo = obtenerTexto(entry, "dc:title");
        String issn = obtenerTexto(entry, "prism:issn");
        String eIssn = obtenerTexto(entry, "prism:eIssn");
        String sourceId = obtenerTexto(entry, "source-id");

        ScopusInfoDTO scopus = new ScopusInfoDTO();
        scopus.setEncontrado(true);
        scopus.setPublisher(obtenerTexto(entry, "dc:publisher"));
        scopus.setSubjectAreas(obtenerSubjectAreas(entry));
        scopus.setAccesoAbierto(obtenerOpenAccess(entry));
        scopus.setTipoOpenAccess(obtenerTexto(entry, "openaccessType"));
        scopus.setTipoFuente(obtenerTexto(entry, "prism:aggregationType"));
        scopus.setCoverageStartYear(obtenerTexto(entry, "coverageStartYear"));
        scopus.setCoverageEndYear(obtenerTexto(entry, "coverageEndYear"));
        scopus.setEnlaceScopus(obtenerEnlaceScopus(entry));

        Scimago encontrado = scimagoPorIssn.get(normalizarIdentificador(issn));
        if (encontrado == null) {
            encontrado = scimagoPorIssn.get(normalizarIdentificador(eIssn));
        }
        if (encontrado == null && normalizarIdentificador(issn).isBlank()
                && normalizarIdentificador(eIssn).isBlank()) {
            encontrado = buscarScimago(null, null, titulo).orElse(null);
        }
        ScimagoInfoDTO scimagoInfo = encontrado == null ? null : mapearScimago(encontrado);

        SerialTitleDTO metricas = null;
        if (scimagoInfo == null) {
            metricas = serialTitleService.extraerMetricas(entry);
            if (metricas != null) {
                scopus.setCuartil(metricas.getQuartile());
                scopus.setBestQuartile(metricas.getBestQuartile());
            }
        }

        ScopusFuente fuente = fuentesPorIssn.get(normalizarIdentificador(issn));
        if (fuente == null) fuente = fuentesPorIssn.get(normalizarIdentificador(eIssn));
        Map<String, JournalInfo> proveedores = null;
        if (fuente != null) {
            Map<String, String> datos = new LinkedHashMap<>();
            if (fuente.getEstado() != null) datos.put("estado", fuente.getEstado());
            datos.put("discontinuada", fuente.isDiscontinuada() ? "Sí" : "No");
            JournalInfo info = new JournalInfo("scopus_excel", null, fuente.getIssn(),
                    fuente.getEissn(), null, null, null, null, null, null, null, null, datos);
            proveedores = Map.of("scopus_excel", info);
        }

        RevistaDTO dto = new RevistaDTO();
        dto.setTitulo(titulo);
        dto.setRevista(titulo);
        dto.setIssn(issn);
        dto.setEIssn(eIssn);
        dto.setSourceId(sourceId);
        dto.setFecha(obtenerTexto(entry, "prism:coverDate"));
        aplicarEnriquecimientoCuartil(dto, scopus, scimagoInfo);
        dto.setScopus(scopus);
        dto.setScimago(scimagoInfo);
        dto.setProveedores(proveedores);
        return dto;
    }

    private Map<String, Scimago> cargarScimagoPagina(Iterable<JsonNode> entries) {
        Set<String> identificadores = new HashSet<>();
        for (JsonNode entry : entries) {
            String issn = normalizarIdentificador(obtenerTexto(entry, "prism:issn"));
            String eIssn = normalizarIdentificador(obtenerTexto(entry, "prism:eIssn"));
            if (!issn.isBlank()) identificadores.add(issn);
            if (!eIssn.isBlank()) identificadores.add(eIssn);
        }
        if (identificadores.isEmpty()) return Map.of();

        String patron = String.join("|", identificadores);
        Map<String, List<Scimago>> candidatos = new LinkedHashMap<>();
        for (Scimago item : scimagoRepository.findAllByIssnNormalizadoPattern(patron)) {
            for (String identificador : extraerIssnsScimago(item.getIssn())) {
                if (identificadores.contains(identificador)) {
                    candidatos.computeIfAbsent(identificador, clave -> new ArrayList<>())
                            .add(item);
                }
            }
        }

        Map<String, Scimago> resultado = new LinkedHashMap<>();
        candidatos.forEach((identificador, items) ->
                seleccionarScimago(items).ifPresent(item -> resultado.put(identificador, item)));
        return resultado;
    }

    private Map<String, ScopusFuente> cargarFuentesPagina(Iterable<JsonNode> entries) {
        if (scopusFuenteRepository == null) return Map.of();
        Set<String> issns = new HashSet<>();
        for (JsonNode entry : entries) {
            String issn = normalizarIdentificador(obtenerTexto(entry, "prism:issn"));
            String eIssn = normalizarIdentificador(obtenerTexto(entry, "prism:eIssn"));
            if (!issn.isBlank()) issns.add(issn);
            if (!eIssn.isBlank()) issns.add(eIssn);
        }
        if (issns.isEmpty()) return Map.of();
        Map<String, ScopusFuente> resultado = new LinkedHashMap<>();
        scopusFuenteRepository.findAllByIssnNormalizadoInOrEissnNormalizadoIn(issns, issns)
                .forEach(item -> {
                    if (item.getIssnNormalizado() != null)
                        resultado.putIfAbsent(item.getIssnNormalizado(), item);
                    if (item.getEissnNormalizado() != null)
                        resultado.putIfAbsent(item.getEissnNormalizado(), item);
                });
        return resultado;
    }

    private RevistaDTO construirDetalleComplementario(
            String sourceId, String issn, String eIssn, String editorial
    ) {
        Optional<Scimago> scimago = buscarScimago(issn, eIssn, null);
        ScimagoInfoDTO scimagoInfo = scimago.map(this::mapearScimago).orElse(null);

        String identificadorMetricas = primerNoVacio(issn, eIssn);
        SerialTitleDTO metricas = identificadorMetricas == null ? null
                : scimagoInfo != null
                ? serialTitleService.obtenerMetricasSinCalcularCuartil(identificadorMetricas)
                : serialTitleService.obtenerMetricas(identificadorMetricas);

        ScopusInfoDTO scopus = new ScopusInfoDTO();
        scopus.setEncontrado(true);
        if (metricas != null) {
            scopus.setPublisher(metricas.getPublisher());
            scopus.setSjr(metricas.getSjr());
            scopus.setSjrYear(metricas.getSjrYear());
            scopus.setSnip(metricas.getSnip());
            scopus.setSnipYear(metricas.getSnipYear());
            scopus.setCiteScore(metricas.getCiteScore());
            scopus.setCiteScoreYear(metricas.getCiteScoreYear());
            scopus.setPercentile(metricas.getPercentile());
            scopus.setBestPercentile(metricas.getBestPercentile());
            scopus.setCuartil(metricas.getQuartile());
            scopus.setBestQuartile(metricas.getBestQuartile());
        }

        RevistaDTO dto = new RevistaDTO();
        dto.setSourceId(sourceId);
        dto.setIssn(issn);
        dto.setEIssn(eIssn);
        aplicarEnriquecimientoCuartil(dto, scopus, scimagoInfo);
        dto.setScopus(scopus);
        dto.setScimago(scimagoInfo);
        complementarProveedores(dto, null, issn, eIssn, editorial);
        return dto;
    }

    private String claveDetalle(String sourceId, String issn, String eIssn) {
        String sourceNormalizado = normalizarIdentificador(sourceId);
        if (!sourceNormalizado.isBlank()) return "source:" + sourceNormalizado;
        String issnNormalizado = normalizarIdentificador(issn);
        if (!issnNormalizado.isBlank()) return "issn:" + issnNormalizado;
        String eIssnNormalizado = normalizarIdentificador(eIssn);
        if (!eIssnNormalizado.isBlank()) return "eissn:" + eIssnNormalizado;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe proporcionar sourceId, ISSN o eISSN.");
    }

    private String primerNoVacio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) return valor;
        }
        return null;
    }

    private ResponseStatusException convertirErrorDetalle(Throwable causa) {
        if (causa instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No fue posible cargar los datos complementarios.", causa);
    }

    private List<String> obtenerCodigosAsjcFacultad(String facultad) {
        if (facultad == null || facultad.isBlank()) return List.of();
        if (facultadScopusAreaRepository == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No está disponible la configuración de facultades.");
        }
        List<String> codigos = facultadScopusAreaRepository
                .findSubareaCodesByFacultad(facultad.trim());
        if (codigos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La facultad indicada no existe o no tiene áreas Scopus configuradas.");
        }
        return codigos;
    }

    public List<Map<String, String>> obtenerFacultades() {
        if (facultadScopusAreaRepository == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No está disponible la configuración de facultades.");
        }
        return facultadScopusAreaRepository.findAllFacultades().stream()
                .map(fila -> Map.of(
                        "codigo", Objects.toString(fila[0], ""),
                        "nombre", Objects.toString(fila[1], "")))
                .toList();
    }

    public List<SubjectAreaDTO> obtenerCamposEstudioFacultad(String facultad) {
        List<String> codigosFacultad = obtenerCodigosAsjcFacultad(facultad);
        String codigosGenerales = codigosFacultad.stream()
                .filter(codigo -> codigo.endsWith("00"))
                .filter(codigo -> !"1000".equals(codigo))
                .collect(java.util.stream.Collectors.joining(","));

        try {
            JsonNode entries = consultarScopus("", 200, false, codigosGenerales);
            Map<String, SubjectAreaDTO> camposUnicos = new LinkedHashMap<>();
            if (entries.isArray()) {
                for (JsonNode entry : entries) {
                    for (SubjectAreaDTO campo : obtenerSubjectAreas(entry)) {
                        if (codigosFacultad.contains(campo.getCodigo())
                                && !"1000".equals(campo.getCodigo())) {
                            camposUnicos.putIfAbsent(campo.getCodigo(), campo);
                        }
                    }
                }
            }
            return camposUnicos.values().stream()
                    .sorted(java.util.Comparator.comparing(
                            SubjectAreaDTO::getNombre,
                            java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible cargar los campos de estudio desde Scopus.", exception);
        }
    }

    private List<String> filtrarCodigosPorCampo(
            List<String> codigosFacultad, String facultad, String campoEstudio
    ) {
        if (facultad == null || facultad.isBlank()
                || campoEstudio == null || campoEstudio.isBlank()) {
            return codigosFacultad;
        }

        String codigo = campoEstudio.trim();
        if (!codigo.matches("\\d{4}") || "1000".equals(codigo)
                || !codigosFacultad.contains(codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El campo de estudio no está asociado a la facultad indicada.");
        }

        return List.of(codigo);
    }

    private JsonNode consultarScopus(
            String termino, Integer cantidad, boolean esIssn, String codigoAsjc) throws Exception {
        boolean sinTermino = termino == null || termino.isBlank();
        String uri = serialUrl + "?count={cantidad}&view=STANDARD"
                + (sinTermino ? "" : esIssn ? "&issn={termino}" : "&title={termino}")
                + (codigoAsjc == null ? "" : "&subjCode={codigoAsjc}");
        String respuesta;
        if (sinTermino) {
            respuesta = restClient.get().uri(uri, cantidad, codigoAsjc)
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json").retrieve().body(String.class);
        } else if (codigoAsjc == null) {
            respuesta = restClient.get().uri(uri, cantidad,
                            esIssn ? normalizarIssn(termino) : termino)
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json").retrieve().body(String.class);
        } else {
            respuesta = restClient.get().uri(uri, cantidad,
                            esIssn ? normalizarIssn(termino) : termino, codigoAsjc)
                    .header("X-ELS-APIKey", apiKey)
                    .header("Accept", "application/json").retrieve().body(String.class);
        }
        return objectMapper.readTree(respuesta)
                .path("serial-metadata-response").path("entry");
    }

    private Map<String, JsonNode> consultarTituloAproximado(
            String titulo, Integer cantidad) throws Exception {
        String tituloNormalizado = normalizarTextoBusqueda(titulo);
        List<String> terminosEncontrados = List.of(tituloNormalizado.split("\\s+")).stream()
                .filter(termino -> !termino.isBlank())
                .filter(termino -> !Set.of("de", "del", "la", "el", "los", "las",
                        "y", "of", "the", "and").contains(termino))
                .distinct()
                .toList();
        final List<String> terminos = terminosEncontrados.isEmpty()
                ? List.of(tituloNormalizado) : terminosEncontrados;

        Map<String, JsonNode> candidatos = new LinkedHashMap<>();
        for (String termino : terminos) {
            agregarEntries(candidatos, consultarScopus(
                    termino, cantidad, false, null), null);
        }

        int minimoCoincidencias = Math.max(1, (terminos.size() + 1) / 2);
        return candidatos.entrySet().stream()
                .filter(entry -> contarCoincidencias(
                        terminos, obtenerTexto(entry.getValue(), "dc:title"))
                        >= minimoCoincidencias)
                .sorted((izquierda, derecha) -> Double.compare(
                        puntuarTitulo(tituloNormalizado, terminos,
                                obtenerTexto(derecha.getValue(), "dc:title")),
                        puntuarTitulo(tituloNormalizado, terminos,
                                obtenerTexto(izquierda.getValue(), "dc:title"))))
                .limit(cantidad)
                .collect(LinkedHashMap::new,
                        (mapa, entry) -> mapa.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private double puntuarTitulo(
            String consulta, List<String> terminos, String titulo) {
        String normalizado = normalizarTextoBusqueda(titulo);
        int coincidencias = contarCoincidencias(terminos, titulo);
        double cobertura = (double) coincidencias / terminos.size();
        double puntuacion = cobertura * 100D;
        if (normalizado.equals(consulta)) puntuacion += 200D;
        else if (normalizado.contains(consulta)) puntuacion += 80D;
        puntuacion += similitud(consulta, normalizado) * 40D;
        return puntuacion;
    }

    private int contarCoincidencias(List<String> terminos, String titulo) {
        List<String> palabrasTitulo = List.of(
                normalizarTextoBusqueda(titulo).split("\\s+"));
        return (int) terminos.stream()
                .filter(termino -> palabrasTitulo.stream()
                        .anyMatch(palabra -> coincidePalabra(termino, palabra)))
                .count();
    }

    private boolean coincidePalabra(String consulta, String titulo) {
        if (consulta.isBlank() || titulo.isBlank()) return false;
        return titulo.contains(consulta) || consulta.contains(titulo)
                || similitud(consulta, titulo) >= 0.75D;
    }

    private double similitud(String izquierda, String derecha) {
        int longitudMaxima = Math.max(izquierda.length(), derecha.length());
        if (longitudMaxima == 0) return 1D;
        return 1D - (double) distanciaLevenshtein(izquierda, derecha) / longitudMaxima;
    }

    private int distanciaLevenshtein(String izquierda, String derecha) {
        int[] anterior = new int[derecha.length() + 1];
        for (int j = 0; j <= derecha.length(); j++) anterior[j] = j;
        for (int i = 1; i <= izquierda.length(); i++) {
            int[] actual = new int[derecha.length() + 1];
            actual[0] = i;
            for (int j = 1; j <= derecha.length(); j++) {
                int costo = izquierda.charAt(i - 1) == derecha.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1,
                        anterior[j] + 1), anterior[j - 1] + costo);
            }
            anterior = actual;
        }
        return anterior[derecha.length()];
    }

    private String normalizarTextoBusqueda(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private JsonNode consultarScopusPaginado(
            String codigoAsjc, int start, int count
    ) throws Exception {
        String respuesta = restClient.get().uri(
                        serialUrl + "?start={start}&count={count}&view=CITESCORE"
                                + "&subjCode={codigoAsjc}",
                        start, count, codigoAsjc)
                .header("X-ELS-APIKey", apiKey)
                .header("Accept", "application/json")
                .retrieve().body(String.class);
        return objectMapper.readTree(respuesta).path("serial-metadata-response");
    }

    private Map<String, JsonNode> deduplicarPagina(
            JsonNode entries, Set<String> codigosPermitidos
    ) {
        Map<String, JsonNode> resultado = new LinkedHashMap<>();
        Set<String> identidades = new HashSet<>();
        if (!entries.isArray()) return resultado;
        for (JsonNode entry : entries) {
            if (obtenerSubjectAreas(entry).stream()
                    .map(SubjectAreaDTO::getCodigo)
                    .noneMatch(codigosPermitidos::contains)) {
                continue;
            }
            List<String> claves = List.of(
                    "source:" + Objects.toString(obtenerTexto(entry, "source-id"), ""),
                    "issn:" + normalizarIdentificador(obtenerTexto(entry, "prism:issn")),
                    "eissn:" + normalizarIdentificador(obtenerTexto(entry, "prism:eIssn"))
            ).stream().filter(clave -> !clave.endsWith(":")).toList();
            if (claves.stream().anyMatch(identidades::contains)) continue;
            identidades.addAll(claves);
            String clavePrincipal = claves.isEmpty()
                    ? "titulo:" + Objects.toString(obtenerTexto(entry, "dc:title"), "")
                    : claves.get(0);
            resultado.putIfAbsent(clavePrincipal, entry);
        }
        return resultado;
    }

    private NavegacionScopus obtenerNavegacion(
            JsonNode raiz, int start, int count, int cantidadRecibida
    ) {
        long total = raiz.path("opensearch:totalResults").asLong(-1);
        boolean hasNext = false;
        Integer ultimoStart = null;
        JsonNode enlaces = raiz.path("link");
        if (enlaces.isArray()) {
            for (JsonNode enlace : enlaces) {
                String ref = obtenerTexto(enlace, "@ref");
                if ("next".equals(ref)) hasNext = true;
                if ("last".equals(ref)) {
                    ultimoStart = extraerStart(obtenerTexto(enlace, "@href"));
                }
            }
        }
        boolean estimado = total < 0;
        if (total < 0 && ultimoStart != null) {
            total = ultimoStart == start && !hasNext
                    ? (long) start + cantidadRecibida
                    : (long) ultimoStart + count;
        } else if (total < 0) {
            total = (long) start + cantidadRecibida + (hasNext ? 1 : 0);
        }
        if (!hasNext) hasNext = (long) start + count < total;
        return new NavegacionScopus(Math.max(total, 0), estimado, hasNext);
    }

    private Integer extraerStart(String enlace) {
        if (enlace == null) return null;
        Matcher matcher = START_EN_ENLACE.matcher(enlace);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private String normalizarIdentificador(String valor) {
        return valor == null ? "" : valor.toUpperCase(Locale.ROOT)
                .replaceAll("[-\\s]", "").trim();
    }

    private record PaginaCampoClave(String campo, int start, int count) { }
    private record PaginaCampoCache(long creadaEnMillis, RevistaPaginaDTO pagina) { }
    private record DetalleCache(long creadoEnMillis, RevistaDTO detalle) { }
    private record NavegacionScopus(long totalResults, boolean totalEstimado,
                                    boolean hasNext) { }

    private void agregarEntries(
            Map<String, JsonNode> destino, JsonNode entries, Set<String> codigosPermitidos) {
        if (!entries.isArray()) return;
        for (JsonNode entry : entries) {
            if (codigosPermitidos != null && obtenerSubjectAreas(entry).stream()
                    .map(SubjectAreaDTO::getCodigo)
                    .noneMatch(codigosPermitidos::contains)) {
                LOGGER.warn("Scopus devolvió la fuente {} fuera de los códigos ASJC solicitados",
                        obtenerTexto(entry, "source-id"));
                continue;
            }
            String clave = Optional.ofNullable(obtenerTexto(entry, "source-id"))
                    .orElseGet(() -> String.join("|",
                            Objects.toString(obtenerTexto(entry, "prism:issn"), ""),
                            Objects.toString(obtenerTexto(entry, "prism:eIssn"), ""),
                            Objects.toString(obtenerTexto(entry, "dc:title"), "")));
            destino.putIfAbsent(clave, entry);
        }
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
        String productId = obtenerProductId(entry);
        String fecha = obtenerTexto(entry, "prism:coverDate");

        // INICIO - Corrección de mapeo Scopus
        String publisher = obtenerTexto(entry, "dc:publisher");
        // FIN - Corrección de mapeo Scopus

        // INICIO - Open Access
        Boolean accesoAbierto = obtenerOpenAccess(entry);
        String tipoOpenAccess = obtenerTexto(entry, "openaccessType");
        // FIN - Open Access

        // INICIO - Tipo de fuente
        String tipoFuente = obtenerTexto(entry, "prism:aggregationType");
        // FIN - Tipo de fuente

        // INICIO - Cobertura
        String coverageStartYear = obtenerTexto(entry, "coverageStartYear");
        String coverageEndYear = obtenerTexto(entry, "coverageEndYear");
        // FIN - Cobertura

        // INICIO - Subject Area
        List<SubjectAreaDTO> subjectAreas = obtenerSubjectAreas(entry);
        // FIN - Subject Area

        String enlaceScopus = obtenerEnlaceScopus(entry);

        Optional<Scimago> scimago = buscarScimago(issn, eIssn, titulo);
        ScimagoInfoDTO scimagoInfo = scimago.map(this::mapearScimago).orElse(null);

        // El cálculo actual de Scopus se conserva exactamente como respaldo,
        // pero solo se ejecuta cuando SCImago no aporta un cuartil oficial.
        String identificadorMetricas = primerNoVacio(issn, eIssn);
        SerialTitleDTO metricas = identificadorMetricas == null ? null
                : scimagoInfo != null
                ? serialTitleService.obtenerMetricasSinCalcularCuartil(identificadorMetricas)
                : serialTitleService.obtenerMetricas(identificadorMetricas);

        ScopusInfoDTO scopusInfo = new ScopusInfoDTO();
        scopusInfo.setEncontrado(true);
        // El campo se conserva por compatibilidad, pero Scopus Serial Title no ofrece país oficial.
        scopusInfo.setPais(null);
        scopusInfo.setAccesoAbierto(accesoAbierto);
        scopusInfo.setEnlaceScopus(enlaceScopus);
        scopusInfo.setPublisher(publisher);
        scopusInfo.setSubjectAreas(subjectAreas);
        scopusInfo.setTipoFuente(tipoFuente);
        scopusInfo.setTipoOpenAccess(tipoOpenAccess);
        scopusInfo.setCoverageStartYear(coverageStartYear);
        scopusInfo.setCoverageEndYear(coverageEndYear);

        if (metricas != null) {

            if (scopusInfo.getPublisher() == null) {
                scopusInfo.setPublisher(metricas.getPublisher());
            }

            scopusInfo.setSjr(metricas.getSjr());
            scopusInfo.setSjrYear(metricas.getSjrYear());

            scopusInfo.setSnip(metricas.getSnip());
            scopusInfo.setSnipYear(metricas.getSnipYear());

            scopusInfo.setCiteScore(metricas.getCiteScore());
            scopusInfo.setCiteScoreYear(metricas.getCiteScoreYear());

            scopusInfo.setPercentile(metricas.getPercentile());
            scopusInfo.setBestPercentile(metricas.getBestPercentile());

            scopusInfo.setCuartil(metricas.getQuartile());
            scopusInfo.setBestQuartile(metricas.getBestQuartile());

        }

        RevistaDTO dto = new RevistaDTO();
        dto.setTitulo(titulo);
        dto.setRevista(revistaNombre);

        // ISSN y eISSN totalmente independientes: cada uno se envía tal cual
        // viene de Scopus, sin usar uno como respaldo del otro.
        dto.setIssn(issn);
        dto.setEIssn(eIssn);

        dto.setSourceId(sourceId);
        dto.setFecha(fecha);

        aplicarEnriquecimientoCuartil(dto, scopusInfo, scimagoInfo);

        dto.setScopus(scopusInfo);
        dto.setScimago(scimagoInfo);
        complementarProveedores(dto, productId, issn, eIssn, publisher);

        return dto;

    }

    // INICIO - Consulta unificada de Springer, DOAJ y futuros proveedores
    private void complementarProveedores(
            RevistaDTO dto, String productId, String issn, String eIssn
    ) {
        complementarProveedores(dto, productId, issn, eIssn, null);
    }

    private void complementarProveedores(
            RevistaDTO dto, String productId, String issn, String eIssn,
            String editorialPrincipal
    ) {
        if (journalProviderRegistry == null) {
            // Compatibilidad con pruebas y consumidores construidos antes del registro Strategy.
            dto.setSpringer(buscarSpringer(productId, issn, eIssn));
            return;
        }
        var proveedores = aplicarPrioridadEditorial(
                journalProviderRegistry.buscarTodos(productId, issn, eIssn),
                editorialPrincipal, issn, eIssn);
        dto.setProveedores(proveedores);
        dto.setDoaj(proveedores.get("doaj"));
        dto.setSpringer(convertirSpringer(proveedores.get("springer")));
    }

    // INICIO - Prioridad Springer para revistas compartidas con DOAJ
    private Map<String, JournalInfo> aplicarPrioridadEditorial(
            Map<String, JournalInfo> encontrados
    ) {
        return aplicarPrioridadEditorial(encontrados, null, null, null);
    }

    /**
     * Resuelve únicamente los datos solapados. Ambos proveedores permanecen
     * visibles; APC y periodicidad se conservan sólo en la fuente prioritaria.
     */
    private Map<String, JournalInfo> aplicarPrioridadEditorial(
            Map<String, JournalInfo> encontrados, String editorialPrincipal
    ) {
        return aplicarPrioridadEditorial(encontrados, editorialPrincipal, null, null);
    }

    /**
     * Selecciona una sola fuente APC. La fuente oficial de la editorial que
     * devuelve Scopus tiene prioridad; el respaldo sólo se usa si aquella no
     * existe o no publica un APC. Toda candidatura debe compartir ISSN/eISSN.
     */
    private Map<String, JournalInfo> aplicarPrioridadEditorial(
            Map<String, JournalInfo> encontrados, String editorialPrincipal,
            String issnConsultado, String eIssnConsultado
    ) {
        if (encontrados == null || encontrados.isEmpty()) {
            return Map.of();
        }

        Map<String, JournalInfo> seleccionados = new LinkedHashMap<>();
        encontrados.forEach((nombre, info) -> {
            if (info != null && coincideIssn(info, issnConsultado, eIssnConsultado)) {
                seleccionados.put(nombre, info);
            }
        });
        if (seleccionados.isEmpty()) return Map.of();
        String proveedorOficial = proveedorDeEditorial(editorialPrincipal);
        String fuenteApc = candidatoValido(
                seleccionados, proveedorOficial, issnConsultado, eIssnConsultado);
        boolean fuenteOficial = fuenteApc != null;

        if (fuenteApc == null) {
            for (String proveedor : ordenRespaldo(seleccionados)) {
                fuenteApc = candidatoValido(
                        seleccionados, proveedor, issnConsultado, eIssnConsultado);
                if (fuenteApc != null) break;
            }
        }

        if (fuenteApc != null) {
            for (var entry : new ArrayList<>(seleccionados.entrySet())) {
                if (entry.getKey().equals(fuenteApc)) {
                    seleccionados.put(entry.getKey(), marcarFuenteApc(
                            entry.getValue(), fuenteOficial ? "editorial-oficial" : "respaldo"));
                } else {
                    seleccionados.put(entry.getKey(), sinDatosEnConflicto(entry.getValue()));
                }
            }
        }
        return seleccionados;
    }

    private String candidatoValido(
            Map<String, JournalInfo> encontrados, String proveedor,
            String issn, String eIssn
    ) {
        if (proveedor == null) return null;
        JournalInfo info = encontrados.get(proveedor);
        return info != null && tieneApc(info)
                && coincideIssn(info, issn, eIssn) ? proveedor : null;
    }

    private boolean tieneApc(JournalInfo info) {
        if (info.getApc() != null && !info.getApc().isBlank()) return true;
        return info.getDatosAdicionales().entrySet().stream().anyMatch(entry ->
                entry.getKey().toLowerCase(Locale.ROOT).startsWith("apc")
                        && entry.getValue() != null && !entry.getValue().isBlank());
    }

    private boolean coincideIssn(JournalInfo info, String issn, String eIssn) {
        Set<String> solicitados = new HashSet<>();
        agregarIssn(solicitados, issn); agregarIssn(solicitados, eIssn);
        if (solicitados.isEmpty()) return false;
        String i = normalizarIssnApc(info.getIssn());
        String e = normalizarIssnApc(info.getEissn());
        return (i != null && solicitados.contains(i))
                || (e != null && solicitados.contains(e));
    }

    private void agregarIssn(Set<String> valores, String valor) {
        String normalizado = normalizarIssnApc(valor);
        if (normalizado != null) valores.add(normalizado);
    }

    private String normalizarIssnApc(String valor) {
        if (valor == null) return null;
        String normalizado = valor.toUpperCase(Locale.ROOT).replaceAll("[^0-9X]", "");
        return normalizado.matches("\\d{7}[\\dX]") ? normalizado : null;
    }

    private List<String> ordenRespaldo(Map<String, JournalInfo> encontrados) {
        List<String> orden = new ArrayList<>(List.of(
                "elsevier", "springer", "wiley", "sage", "cambridge", "ieee",
                "degruyter", "brill", "doaj"));
        encontrados.keySet().stream().filter(k -> !orden.contains(k)).sorted().forEach(orden::add);
        return orden;
    }

    private String proveedorDeEditorial(String editorial) {
        if (editorial == null || editorial.isBlank()) return null;
        String valor = editorial.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
        if (valor.contains("elsevier") || valor.contains("cell press")) return "elsevier";
        if (valor.contains("springer") || valor.contains("springer nature")
                || valor.contains("springeropen") || valor.matches(".*\\bbmc\\b.*")
                || valor.contains("nature portfolio")) return "springer";
        if (valor.contains("john wiley") || valor.matches(".*\\bwiley\\b.*")) return "wiley";
        if (valor.matches(".*\\bsage\\b.*")) return "sage";
        if (valor.contains("cambridge university press")) return "cambridge";
        if (valor.contains("de gruyter") || valor.contains("walter de gruyter")) return "degruyter";
        if (valor.matches(".*\\bbrill\\b.*")) return "brill";
        if (valor.matches(".*\\bieee\\b.*")
                || valor.contains("institute of electrical and electronics engineers")) return "ieee";
        return null;
    }

    private JournalInfo marcarFuenteApc(JournalInfo info, String prioridad) {
        Map<String, String> adicionales = new LinkedHashMap<>(info.getDatosAdicionales());
        adicionales.put("fuenteApcSeleccionada", info.getProveedor());
        adicionales.put("prioridadApc", prioridad);
        return copiarJournalInfo(info, info.getApc(), info.getMonedaApc(), adicionales);
    }

    private JournalInfo sinDatosEnConflicto(JournalInfo info) {
        Map<String, String> adicionales =
                new LinkedHashMap<>(info.getDatosAdicionales());
        adicionales.entrySet().removeIf(entry -> {
            String clave = entry.getKey().toLowerCase(Locale.ROOT);
            return clave.startsWith("apc") || clave.equals("preciolista")
                    || clave.equals("precioactual") || clave.equals("moneda");
        });
        adicionales.put("apcDescartadoPorPrioridad", "true");
        return copiarJournalInfo(info, null, null, adicionales);
    }

    private JournalInfo copiarJournalInfo(
            JournalInfo info, String apc, String moneda,
            Map<String, String> adicionales
    ) {
        return new JournalInfo(
                info.getProveedor(), info.getTitulo(), info.getIssn(),
                info.getEissn(), info.getEditorial(), info.getPais(),
                info.getIdiomas(), info.getMaterias(), info.getLicencia(),
                apc, moneda, info.getUrl(), adicionales);
    }

    private boolean esEditorialSpringer(String editorial) {
        if (editorial == null || editorial.isBlank()) {
            return false;
        }
        String valor = editorial.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
        return "springer".equals(valor)
                || valor.contains("springer nature")
                || valor.contains("springeropen")
                || valor.contains("springer open")
                || valor.matches(".*\\bbmc\\b.*");
    }
    // FIN - Prioridad Springer para revistas compartidas con DOAJ

    private SpringerInfoDTO convertirSpringer(JournalInfo info) {
        if (info == null) return null;
        var datos = info.getDatosAdicionales();
        return new SpringerInfoDTO(
                info.getTitulo(), info.getIssn(), info.getEissn(),
                info.getEditorial(), datos.get("modeloPublicacion"),
                datos.get("tipoHibrido"), info.getIdiomas(),
                convertirEntero(datos.get("cantidadVolumenes")),
                convertirEntero(datos.get("numerosProgramados")),
                datos.get("comentarios"), info.getUrl(),
                datos.get("apcEur"), datos.get("apcUsd"),
                datos.get("apcGbp"), datos.get("apcWebsite"),
                datos.get("periodicidadEstimada"),
                datos.get("estadoSpringer"),
                convertirEntero(datos.get("numerosPorVolumen"))
        );
    }

    private Integer convertirEntero(String valor) {
        try {
            return valor == null ? null : Integer.valueOf(valor);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
    // FIN - Consulta unificada de Springer, DOAJ y futuros proveedores

    private Optional<Scimago> buscarScimago(String issn, String eIssn, String titulo) {
        String issnNormalizado = normalizarIdentificador(issn);
        String eIssnNormalizado = normalizarIdentificador(eIssn);

        if (!issnNormalizado.isBlank()) {
            Optional<Scimago> coincidencia = buscarScimagoPorIdentificador(issnNormalizado);
            if (coincidencia.isPresent()) return coincidencia;
        }
        if (!eIssnNormalizado.isBlank()) {
            Optional<Scimago> coincidencia = buscarScimagoPorIdentificador(eIssnNormalizado);
            if (coincidencia.isPresent()) return coincidencia;
        }

        // Un título nunca sustituye a identificadores disponibles que no coincidieron.
        if (!issnNormalizado.isBlank() || !eIssnNormalizado.isBlank()
                || titulo == null || titulo.isBlank()) {
            return Optional.empty();
        }

        String tituloNormalizado = normalizarTextoBusqueda(titulo);
        return seleccionarScimago(scimagoRepository.findAll().stream()
                .filter(item -> tituloNormalizado.equals(
                        normalizarTextoBusqueda(item.getTitle())))
                .toList());
    }

    private Optional<Scimago> buscarScimagoPorIdentificador(String identificador) {
        List<Scimago> candidatos = scimagoRepository
                .findAllByIssnNormalizadoPattern(identificador).stream()
                .filter(item -> extraerIssnsScimago(item.getIssn()).contains(identificador))
                .toList();
        return seleccionarScimago(candidatos);
    }

    private Set<String> extraerIssnsScimago(String valor) {
        if (valor == null || valor.isBlank()) return Set.of();
        Set<String> resultado = new HashSet<>();
        for (String parte : valor.split("[,;\\s]+")) {
            String normalizado = normalizarIdentificador(parte);
            if (!normalizado.isBlank()) resultado.add(normalizado);
        }
        return resultado;
    }

    private Optional<Scimago> seleccionarScimago(List<Scimago> candidatos) {
        return candidatos.stream().min(
                Comparator.comparingInt(this::anioMasReciente).reversed()
                        .thenComparingInt(item -> prioridadCuartil(item.getSJRBestQuartile()))
                        .thenComparing(item -> item.getSourceid() == null
                                ? Double.MAX_VALUE : item.getSourceid())
        );
    }

    private int anioMasReciente(Scimago item) {
        int anio = 0;
        Matcher matcher = ANIO.matcher(item.getCoverage() == null ? "" : item.getCoverage());
        while (matcher.find()) anio = Math.max(anio, Integer.parseInt(matcher.group()));
        return anio;
    }

    private int prioridadCuartil(String cuartil) {
        return switch (cuartil == null ? "" : cuartil.trim().toUpperCase(Locale.ROOT)) {
            case "Q1" -> 1;
            case "Q2" -> 2;
            case "Q3" -> 3;
            case "Q4" -> 4;
            default -> 5;
        };
    }

    private void aplicarEnriquecimientoCuartil(
            RevistaDTO dto, ScopusInfoDTO scopus, ScimagoInfoDTO scimago
    ) {
        if (scimago != null) {
            dto.setPais(scimago.getPais());
            dto.setCuartil(scimago.getCuartil());
            dto.setOrigenCuartil(CUARTIL_SCIMAGO);
            dto.setEncontradaEnScimago(true);
            return;
        }

        String cuartil = scopus == null ? null : scopus.getCuartil();
        dto.setPais(scopus == null ? null : scopus.getPais());
        dto.setCuartil(cuartil);
        dto.setOrigenCuartil(cuartil == null || cuartil.isBlank()
                ? CUARTIL_NO_DISPONIBLE : CUARTIL_SCOPUS_CALCULADO);
        dto.setEncontradaEnScimago(false);
    }

    // INICIO - Integración Springer en búsquedas
    private SpringerInfoDTO buscarSpringer(
            String productId,
            String issn,
            String eIssn
    ) {
        try {
            return springerRevistaService
                    .buscarParaBusqueda(productId, issn, eIssn)
                    .orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "No fue posible complementar la búsqueda con Springer: {}",
                    exception.getMessage()
            );
            return null;
        }
    }

    private String obtenerProductId(JsonNode entry) {
        String productId = obtenerTexto(entry, "product-id");
        return productId != null
                ? productId
                : obtenerTexto(entry, "productId");
    }
    // FIN - Integración Springer en búsquedas

    private ScimagoInfoDTO mapearScimago(Scimago s) {

        ScimagoInfoDTO info = new ScimagoInfoDTO();
        info.setEncontrado(true);
        info.setPais(s.getCountry());
        info.setCuartil(s.getSJRBestQuartile());

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

    // INICIO - Subject Area
    private List<SubjectAreaDTO> obtenerSubjectAreas(JsonNode entry) {

        List<SubjectAreaDTO> areas = new ArrayList<>();
        JsonNode subjectAreas = entry.path("subject-area");

        if (subjectAreas.isArray()) {
            for (JsonNode area : subjectAreas) {
                areas.add(new SubjectAreaDTO(
                        obtenerTexto(area, "@code"),
                        obtenerTexto(area, "@abbrev"),
                        obtenerTexto(area, "$")
                ));
            }
        }

        return areas;

    }
    // FIN - Subject Area

    // INICIO - Open Access
    private Boolean obtenerOpenAccess(JsonNode entry) {

        JsonNode openaccess = entry.get("openaccess");

        if (openaccess == null || openaccess.isNull()) {
            return false;
        }

        if (openaccess.isBoolean()) {
            return openaccess.asBoolean();
        }

        String valor = openaccess.asText();
        return "1".equals(valor) || "true".equalsIgnoreCase(valor);

    }
    // FIN - Open Access

    private String obtenerEnlaceScopus(JsonNode entry) {

        JsonNode links = entry.path("link");

        if (links.isArray()) {

            for (JsonNode link : links) {

                String ref = obtenerTexto(
                        link,
                        "@ref"
                );

                // INICIO - Corrección de mapeo Scopus
                if ("scopus-source".equals(ref) || "scopus".equals(ref)) {

                    return obtenerTexto(
                            link,
                            "@href"
                    );

                }
                // FIN - Corrección de mapeo Scopus

            }

        }

        return null;

    }

}
