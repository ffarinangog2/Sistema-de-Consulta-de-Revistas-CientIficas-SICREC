package uteq.edu.ec.crecuteq.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaRequestDTO;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaResponseDTO;
import uteq.edu.ec.crecuteq.dto.SpringerInfoDTO;
import uteq.edu.ec.crecuteq.dto.SpringerApcRequestDTO;
import uteq.edu.ec.crecuteq.entity.SpringerRevista;
import uteq.edu.ec.crecuteq.repository.SpringerRevistaRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

// INICIO - Módulo Springer
@Service
public class SpringerRevistaService {

    private final SpringerRevistaRepository springerRevistaRepository;

    public SpringerRevistaService(SpringerRevistaRepository springerRevistaRepository) {
        this.springerRevistaRepository = springerRevistaRepository;
    }

    @Transactional(readOnly = true)
    public List<SpringerRevistaResponseDTO> listar() {
        return springerRevistaRepository.findAll()
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpringerRevistaResponseDTO buscarPorId(Long id) {
        return convertirDTO(obtenerEntidad(id));
    }

    // INICIO - Integración Springer en búsquedas
    @Transactional(readOnly = true)
    public Optional<SpringerInfoDTO> buscarParaBusqueda(
            String productId, String issn, String eissn
    ) {
        String productIdNormalizado = normalizarTexto(productId);
        String issnNormalizado = normalizarIssnParaBusqueda(issn);
        String eissnNormalizado = normalizarIssnParaBusqueda(eissn);
        Optional<SpringerRevista> coincidencia = Optional.empty();

        if (productIdNormalizado != null) {
            coincidencia = springerRevistaRepository
                    .findFirstByProductIdIgnoreCase(productIdNormalizado);
        }
        if (coincidencia.isEmpty() && issnNormalizado != null) {
            coincidencia = springerRevistaRepository
                    .findFirstByIssnNormalizado(issnNormalizado);
        }
        if (coincidencia.isEmpty() && eissnNormalizado != null) {
            coincidencia = springerRevistaRepository
                    .findFirstByEissnNormalizado(eissnNormalizado);
        }
        return coincidencia.map(this::convertirInfoBusqueda);
    }

    private String normalizarIssnParaBusqueda(String valor) {
        String normalizado = normalizarTexto(valor);
        if (normalizado == null) {
            return null;
        }
        normalizado = normalizado.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "");
        return normalizado.matches("\\d{7}[\\dX]") ? normalizado : null;
    }

    private SpringerInfoDTO convertirInfoBusqueda(SpringerRevista revista) {
        return new SpringerInfoDTO(
                revista.getTitulo(), revista.getIssn(), revista.getEissn(),
                revista.getImprint(), revista.getPublishingModel(),
                revista.getHybridType(), revista.getPrimaryLanguage(),
                null, revista.getNumerosProgramados(), revista.getComentarios(),
                revista.getUrlOficial(),
                revista.getApcEur(), revista.getApcUsd(),
                revista.getApcGbp(), revista.getApcWebsite(),
                calcularPeriodicidad(revista.getNumerosPorVolumen()),
                determinarEstadoSpringer(revista.getComentarios()),
                revista.getNumerosPorVolumen()
        );
    }

    private String calcularPeriodicidad(Integer numerosPorVolumen) {
        if (numerosPorVolumen == null || numerosPorVolumen <= 0) {
            return null;
        }
        return switch (numerosPorVolumen) {
            case 12 -> "Mensual";
            case 6 -> "Bimestral";
            case 4 -> "Trimestral";
            case 2 -> "Semestral";
            case 1 -> "Anual";
            default -> "Frecuencia: " + numerosPorVolumen + " números por volumen";
        };
    }

    private String determinarEstadoSpringer(String comentarios) {
        String valor = comentarios == null
                ? ""
                : comentarios.toLowerCase(Locale.ROOT);
        return valor.contains("no longer published")
                || valor.contains("no longer distributed")
                ? "Descontinuada en Springer"
                : "Activa en Springer";
    }
    // FIN - Integración Springer en búsquedas

    @Transactional
    public SpringerRevistaResponseDTO crear(SpringerRevistaRequestDTO request) {
        String issnNormalizado = normalizarYValidarIssn(request.getIssn(), "ISSN");
        String eissnNormalizado = normalizarYValidarIssn(request.getEissn(), "eISSN");
        String productId = normalizarTexto(request.getProductId());

        validarIdentificadoresDistintos(issnNormalizado, eissnNormalizado);
        validarDuplicados(null, issnNormalizado, eissnNormalizado, productId);

        SpringerRevista revista = new SpringerRevista();
        aplicarDatos(revista, request, issnNormalizado, eissnNormalizado, productId);
        return convertirDTO(springerRevistaRepository.save(revista));
    }

    @Transactional
    public SpringerRevistaResponseDTO actualizar(Long id, SpringerRevistaRequestDTO request) {
        SpringerRevista revista = obtenerEntidad(id);
        String issnNormalizado = normalizarYValidarIssn(request.getIssn(), "ISSN");
        String eissnNormalizado = normalizarYValidarIssn(request.getEissn(), "eISSN");
        String productId = normalizarTexto(request.getProductId());

        validarIdentificadoresDistintos(issnNormalizado, eissnNormalizado);
        validarDuplicados(id, issnNormalizado, eissnNormalizado, productId);

        aplicarDatos(revista, request, issnNormalizado, eissnNormalizado, productId);
        return convertirDTO(springerRevistaRepository.save(revista));
    }

    @Transactional
    public void eliminar(Long id) {
        springerRevistaRepository.delete(obtenerEntidad(id));
    }

    // INICIO - Importación Springer
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean importarFila(SpringerRevistaRequestDTO request) {
        String issnNormalizado = normalizarYValidarIssn(request.getIssn(), "ISSN");
        String eissnNormalizado = normalizarYValidarIssn(request.getEissn(), "eISSN");
        String productId = normalizarTexto(request.getProductId());

        validarIdentificadoresDistintos(issnNormalizado, eissnNormalizado);

        SpringerRevista revista = buscarParaImportacion(
                productId,
                issnNormalizado,
                eissnNormalizado
        );
        boolean insertado = revista == null;

        if (insertado) {
            revista = new SpringerRevista();
        }

        aplicarDatosImportacion(
                revista,
                request,
                issnNormalizado,
                eissnNormalizado,
                productId
        );
        springerRevistaRepository.saveAndFlush(revista);
        return insertado;
    }

    private SpringerRevista buscarParaImportacion(
            String productId,
            String issnNormalizado,
            String eissnNormalizado
    ) {
        if (productId != null) {
            var porProductId = springerRevistaRepository
                    .findFirstByProductIdIgnoreCase(productId);
            if (porProductId.isPresent()) {
                return porProductId.get();
            }
        }

        if (issnNormalizado != null) {
            var porIssn = springerRevistaRepository
                    .findFirstByIssnNormalizado(issnNormalizado);
            if (porIssn.isPresent()) {
                return porIssn.get();
            }
        }

        if (eissnNormalizado != null) {
            return springerRevistaRepository
                    .findFirstByEissnNormalizado(eissnNormalizado)
                    .orElse(null);
        }

        return null;
    }

    private void aplicarDatosImportacion(
            SpringerRevista revista,
            SpringerRevistaRequestDTO request,
            String issnNormalizado,
            String eissnNormalizado,
            String productId
    ) {
        revista.setTitulo(request.getTitulo().trim());

        if (issnNormalizado != null) {
            revista.setIssn(formatearIssn(issnNormalizado));
            revista.setIssnNormalizado(issnNormalizado);
        }

        if (eissnNormalizado != null) {
            revista.setEissn(formatearIssn(eissnNormalizado));
            revista.setEissnNormalizado(eissnNormalizado);
        }

        if (productId != null) {
            revista.setProductId(productId);
        }

        if (normalizarTexto(request.getImprint()) != null) {
            revista.setImprint(normalizarTexto(request.getImprint()));
        }

        if (normalizarTexto(request.getPublishingModel()) != null) {
            revista.setPublishingModel(normalizarTexto(request.getPublishingModel()));
        }

        if (normalizarTexto(request.getHybridType()) != null) {
            revista.setHybridType(normalizarTexto(request.getHybridType()));
        }

        if (normalizarTexto(request.getPrimaryLanguage()) != null) {
            revista.setPrimaryLanguage(normalizarTexto(request.getPrimaryLanguage()));
        }

        revista.setNumerosPorVolumen(request.getNumerosPorVolumen());
        revista.setNumerosProgramados(request.getNumerosProgramados());
        revista.setComentarios(normalizarTexto(request.getComentarios()));
        revista.setFuenteArchivo(normalizarTexto(request.getFuenteArchivo()));
    }
    // FIN - Importación Springer

    // INICIO - Importación PDF Springer
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean actualizarApc(SpringerApcRequestDTO request) {
        String eissnNormalizado = normalizarYValidarIssn(
                request.getEissn(), "eISSN");
        String issnNormalizado = normalizarYValidarIssn(
                request.getIssn(), "ISSN");

        SpringerRevista revista = buscarParaApc(
                eissnNormalizado,
                issnNormalizado
        );

        if (revista == null) {
            return false;
        }

        revista.setApcEur(conservarValorApc(request.getApcEur()));
        revista.setApcUsd(conservarValorApc(request.getApcUsd()));
        revista.setApcGbp(conservarValorApc(request.getApcGbp()));
        revista.setApcWebsite(conservarValorApc(request.getUrlOficial()));
        if (request.getAnioVigencia() != null) {
            revista.setAnioVigenciaApc(request.getAnioVigencia());
        }

        springerRevistaRepository.saveAndFlush(revista);
        return true;
    }

    private SpringerRevista buscarParaApc(
            String eissnNormalizado,
            String issnNormalizado
    ) {
        if (eissnNormalizado != null) {
            var porEissn = springerRevistaRepository
                    .findFirstByEissnNormalizado(eissnNormalizado);
            if (porEissn.isPresent()) {
                return porEissn.get();
            }
        }

        if (issnNormalizado != null) {
            return springerRevistaRepository
                    .findFirstByIssnNormalizado(issnNormalizado)
                    .orElse(null);
        }
        return null;
    }

    private String conservarValorApc(String valor) {
        return valor == null ? null : valor.trim();
    }
    // FIN - Importación PDF Springer

    private SpringerRevista obtenerEntidad(Long id) {
        return springerRevistaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la revista Springer indicada"
                ));
    }

    private void aplicarDatos(
            SpringerRevista revista,
            SpringerRevistaRequestDTO request,
            String issnNormalizado,
            String eissnNormalizado,
            String productId
    ) {
        revista.setTitulo(request.getTitulo().trim());
        revista.setIssn(formatearIssn(issnNormalizado));
        revista.setIssnNormalizado(issnNormalizado);
        revista.setEissn(formatearIssn(eissnNormalizado));
        revista.setEissnNormalizado(eissnNormalizado);
        revista.setProductId(productId);
        revista.setImprint(normalizarTexto(request.getImprint()));
        revista.setPublishingModel(normalizarTexto(request.getPublishingModel()));
        revista.setHybridType(normalizarTexto(request.getHybridType()));
        revista.setPrimaryLanguage(normalizarTexto(request.getPrimaryLanguage()));
        revista.setUrlOficial(normalizarTexto(request.getUrlOficial()));
        revista.setFuenteArchivo(normalizarTexto(request.getFuenteArchivo()));
        revista.setNumerosPorVolumen(request.getNumerosPorVolumen());
        revista.setNumerosProgramados(request.getNumerosProgramados());
        revista.setComentarios(normalizarTexto(request.getComentarios()));
    }

    private void validarDuplicados(
            Long id,
            String issnNormalizado,
            String eissnNormalizado,
            String productId
    ) {
        boolean duplicado = id == null
                ? springerRevistaRepository.existeDuplicado(
                        issnNormalizado, eissnNormalizado, productId)
                : springerRevistaRepository.existeDuplicadoExcluyendoId(
                        id, issnNormalizado, eissnNormalizado, productId);

        if (duplicado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una revista Springer con el ISSN, eISSN o Product ID indicado"
            );
        }
    }

    private void validarIdentificadoresDistintos(String issn, String eissn) {
        if (issn != null && issn.equals(eissn)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ISSN y el eISSN deben ser identificadores distintos"
            );
        }
    }

    private String normalizarYValidarIssn(String valor, String nombreCampo) {
        String normalizado = normalizarTexto(valor);

        if (normalizado == null) {
            return null;
        }

        normalizado = normalizado
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "");

        if (!normalizado.matches("\\d{7}[\\dX]") || !tieneDigitoControlValido(normalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    nombreCampo + " no tiene un formato o dígito de control válido"
            );
        }

        return normalizado;
    }

    private boolean tieneDigitoControlValido(String issn) {
        int suma = 0;

        for (int indice = 0; indice < 7; indice++) {
            suma += Character.digit(issn.charAt(indice), 10) * (8 - indice);
        }

        int resultado = (11 - (suma % 11)) % 11;
        char esperado = resultado == 10
                ? 'X'
                : Character.forDigit(resultado, 10);

        return issn.charAt(7) == esperado;
    }

    private String formatearIssn(String normalizado) {
        return normalizado == null
                ? null
                : normalizado.substring(0, 4) + "-" + normalizado.substring(4);
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private SpringerRevistaResponseDTO convertirDTO(SpringerRevista revista) {
        return new SpringerRevistaResponseDTO(
                revista.getId(),
                revista.getTitulo(),
                revista.getIssn(),
                revista.getIssnNormalizado(),
                revista.getEissn(),
                revista.getEissnNormalizado(),
                revista.getProductId(),
                revista.getImprint(),
                revista.getPublishingModel(),
                revista.getHybridType(),
                revista.getPrimaryLanguage(),
                revista.getUrlOficial(),
                revista.getFuenteArchivo(),
                revista.getApcEur(),
                revista.getApcUsd(),
                revista.getApcGbp(),
                revista.getApcWebsite(),
                revista.getAnioVigenciaApc(),
                revista.getFechaImportacion(),
                revista.getFechaActualizacion()
        );
    }
}
// FIN - Módulo Springer
