package uteq.edu.ec.crecuteq.service;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaRequestDTO;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// INICIO - Importación Springer
@Service
public class SpringerImportService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringerImportService.class);

    private static final String TITLE = "title";
    private static final String PRODUCT_ID = "product id";
    private static final String ISSN_PRINT = "issn print";
    private static final String ISSN_ELECTRONIC = "issn electronic";
    private static final String IMPRINT = "imprint";
    private static final String PUBLISHING_MODEL = "publishing model";
    private static final String HYBRID_TYPE = "hybrid type";
    private static final String PRIMARY_LANGUAGE = "primary language";
    private static final String SINGLE_ISSUES_PER_VOLUME = "single issues per volume";
    private static final String SCHEDULED_VOL_NOS = "scheduled vol nos";
    private static final String COMMENTS = "comments";

    private static final Set<String> COLUMNAS_OFICIALES = Set.of(
            TITLE,
            PRODUCT_ID,
            ISSN_PRINT,
            ISSN_ELECTRONIC,
            IMPRINT,
            PUBLISHING_MODEL,
            HYBRID_TYPE,
            PRIMARY_LANGUAGE,
            SINGLE_ISSUES_PER_VOLUME,
            SCHEDULED_VOL_NOS,
            COMMENTS
    );

    private final SpringerRevistaService springerRevistaService;
    private final AuditoriaService auditoriaService;

    public SpringerImportService(
            SpringerRevistaService springerRevistaService,
            AuditoriaService auditoriaService
    ) {
        this.springerRevistaService = springerRevistaService;
        this.auditoriaService = auditoriaService;
    }

    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe proporcionar un archivo Excel"
            );
        }

        long inicio = System.nanoTime();
        int leidos = 0;
        int insertados = 0;
        int actualizados = 0;
        int rechazados = 0;

        try (InputStream entrada = archivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(entrada)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw archivoInvalido("El archivo Excel no contiene hojas");
            }

            Sheet hoja = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper()
                    .createFormulaEvaluator();
            Encabezado encabezado = detectarEncabezado(hoja, formatter, evaluator);

            for (int numeroFila = encabezado.numeroFila() + 1;
                 numeroFila <= hoja.getLastRowNum();
                 numeroFila++) {

                Row fila = hoja.getRow(numeroFila);
                if (fila == null || filaVacia(
                        fila, encabezado.columnas(), formatter, evaluator)) {
                    continue;
                }

                leidos++;

                try {
                    SpringerRevistaRequestDTO request = construirRequest(
                            fila,
                            encabezado.columnas(),
                            formatter,
                            evaluator,
                            archivo.getOriginalFilename()
                    );

                    boolean insertado = springerRevistaService.importarFila(request);
                    if (insertado) {
                        insertados++;
                    } else {
                        actualizados++;
                    }
                } catch (Exception exception) {
                    rechazados++;
                    LOGGER.warn(
                            "Importación Springer: fila {} rechazada: {}",
                            numeroFila + 1,
                            obtenerMensaje(exception)
                    );
                }
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw archivoInvalido(
                    "No fue posible leer el archivo Excel: "
                            + obtenerMensaje(exception)
            );
        }

        long tiempoEjecucionMs =
                (System.nanoTime() - inicio) / 1_000_000;

        return new CatalogImportResponseDTO(
                leidos,
                insertados,
                actualizados,
                rechazados,
                tiempoEjecucionMs
        );
    }

    private Encabezado detectarEncabezado(
            Sheet hoja,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        Encabezado mejorEncabezado = null;

        for (Row fila : hoja) {
            Map<String, Integer> columnas = new HashMap<>();

            for (var celda : fila) {
                String nombre = normalizarEncabezado(
                        formatter.formatCellValue(celda, evaluator)
                );
                if (COLUMNAS_OFICIALES.contains(nombre)) {
                    columnas.putIfAbsent(nombre, celda.getColumnIndex());
                }
            }

            if (!columnas.isEmpty()
                    && (mejorEncabezado == null
                    || columnas.size() > mejorEncabezado.columnas().size())) {
                mejorEncabezado = new Encabezado(
                        fila.getRowNum(),
                        Map.copyOf(columnas)
                );
            }

            if (columnas.size() == COLUMNAS_OFICIALES.size()) {
                break;
            }
        }

        if (mejorEncabezado == null) {
            throw archivoInvalido(
                    "No se encontró una fila con encabezados oficiales de Springer"
            );
        }

        return mejorEncabezado;
    }

    private SpringerRevistaRequestDTO construirRequest(
            Row fila,
            Map<String, Integer> columnas,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            String nombreArchivo
    ) {
        SpringerRevistaRequestDTO request = new SpringerRevistaRequestDTO();
        request.setTitulo(leerValor(
                fila, columnas.get(TITLE), formatter, evaluator));
        request.setProductId(leerValor(
                fila, columnas.get(PRODUCT_ID), formatter, evaluator));
        request.setIssn(leerValor(
                fila, columnas.get(ISSN_PRINT), formatter, evaluator));
        request.setEissn(leerValor(
                fila, columnas.get(ISSN_ELECTRONIC), formatter, evaluator));
        request.setImprint(leerValor(
                fila, columnas.get(IMPRINT), formatter, evaluator));
        request.setPublishingModel(leerValor(
                fila, columnas.get(PUBLISHING_MODEL), formatter, evaluator));
        request.setHybridType(leerValor(
                fila, columnas.get(HYBRID_TYPE), formatter, evaluator));
        request.setPrimaryLanguage(leerValor(
                fila, columnas.get(PRIMARY_LANGUAGE), formatter, evaluator));
        request.setNumerosPorVolumen(leerEntero(
                fila, columnas.get(SINGLE_ISSUES_PER_VOLUME), formatter, evaluator));
        request.setNumerosProgramados(leerEntero(
                fila, columnas.get(SCHEDULED_VOL_NOS), formatter, evaluator));
        request.setComentarios(leerValor(
                fila, columnas.get(COMMENTS), formatter, evaluator));
        request.setFuenteArchivo(normalizarTexto(nombreArchivo));

        if (request.getTitulo() == null) {
            throw new IllegalArgumentException(
                    "La fila no contiene el título oficial de la revista"
            );
        }

        return request;
    }

    private boolean filaVacia(
            Row fila,
            Map<String, Integer> columnas,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        return columnas.values()
                .stream()
                .allMatch(indice ->
                        leerValor(fila, indice, formatter, evaluator) == null);
    }

    private String leerValor(
            Row fila,
            Integer indice,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        if (indice == null) {
            return null;
        }

        var celda = fila.getCell(indice, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return celda == null
                ? null
                : normalizarTexto(formatter.formatCellValue(celda, evaluator));
    }

    private Integer leerEntero(
            Row fila,
            Integer indice,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        String valor = leerValor(fila, indice, formatter, evaluator);
        if (valor == null || !valor.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizarEncabezado(String valor) {
        String normalizado = normalizarTexto(valor);
        return normalizado == null
                ? ""
                : normalizado.toLowerCase(Locale.ROOT)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String obtenerMensaje(Exception exception) {
        if (exception instanceof ResponseStatusException response
                && response.getReason() != null) {
            return response.getReason();
        }

        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private ResponseStatusException archivoInvalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private record Encabezado(
            int numeroFila,
            Map<String, Integer> columnas
    ) {
    }
}
// FIN - Importación Springer
