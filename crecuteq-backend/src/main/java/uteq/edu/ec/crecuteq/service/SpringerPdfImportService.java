package uteq.edu.ec.crecuteq.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.SpringerApcRequestDTO;
import uteq.edu.ec.crecuteq.dto.SpringerPdfImportResponseDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// INICIO - Importación PDF Springer
@Service
public class SpringerPdfImportService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringerPdfImportService.class);

    private static final Pattern ISSN_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\d{4}-\\d{3}[\\dXx]|\\d{7}[\\dXx](?!\\d))");
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern APC_PATTERN = Pattern.compile(
            "(?i)see\\s+website|not\\s+applicable|no\\s+apc|n/?a|free|"
                    + "\\d[\\d,]*(?:\\.\\d+)?(?:\\s+RSF)?"
    );
    private static final Pattern YEAR_PATTERN =
            Pattern.compile("\\b(20\\d{2})\\b");

    private static final List<String> IMPRINTS = List.of(
            "Science Press, co-published with Springer",
            "Akadémiai Kiadó, co-published with Springer",
            "Springer Nature",
            "Nature Portfolio",
            "Palgrave Macmillan",
            "BioMed Central",
            "SpringerMedizin",
            "Birkhäuser",
            "Springer",
            "Adis"
    );

    private final SpringerRevistaService springerRevistaService;
    private final AuditoriaService auditoriaService;

    public SpringerPdfImportService(
            SpringerRevistaService springerRevistaService,
            AuditoriaService auditoriaService
    ) {
        this.springerRevistaService = springerRevistaService;
        this.auditoriaService = auditoriaService;
    }

    public SpringerPdfImportResponseDTO importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw archivoInvalido("Debe proporcionar un archivo PDF");
        }

        long inicio = System.nanoTime();
        int actualizados = 0;
        int rechazados;
        int sinCoincidencia = 0;
        ParseoPdf parseo;

        try {
            byte[] contenido = archivo.getBytes();
            validarFirmaPdf(contenido);
            parseo = extraerRegistros(contenido);
        } catch (IOException | RuntimeException exception) {
            throw archivoInvalido(
                    "No fue posible leer el PDF: " + obtenerMensaje(exception)
            );
        }

        rechazados = parseo.rechazados();

        for (SpringerApcRequestDTO registro : parseo.registros()) {
            try {
                boolean actualizado = springerRevistaService.actualizarApc(registro);
                if (actualizado) {
                    actualizados++;
                } else {
                    sinCoincidencia++;
                    LOGGER.warn(
                            "Importación PDF Springer sin coincidencia: título='{}', eISSN='{}'",
                            registro.getTitulo(),
                            registro.getEissn()
                    );
                }
            } catch (Exception exception) {
                rechazados++;
                LOGGER.warn(
                        "Importación PDF Springer rechazada: título='{}', eISSN='{}': {}",
                        registro.getTitulo(),
                        registro.getEissn(),
                        obtenerMensaje(exception)
                );
            }
        }

        long tiempoEjecucionMs =
                (System.nanoTime() - inicio) / 1_000_000;

        return new SpringerPdfImportResponseDTO(
                parseo.registros().size() + parseo.rechazados(),
                actualizados,
                rechazados,
                sinCoincidencia,
                tiempoEjecucionMs
        );
    }

    private void validarFirmaPdf(byte[] contenido) {
        if (contenido.length < 5
                || contenido[0] != '%'
                || contenido[1] != 'P'
                || contenido[2] != 'D'
                || contenido[3] != 'F'
                || contenido[4] != '-') {
            throw archivoInvalido("El archivo proporcionado no es un PDF válido");
        }
    }

    ParseoPdf extraerRegistros(byte[] contenido) throws IOException {
        try (PdfReader reader = new PdfReader(contenido)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            List<String> paginas = new ArrayList<>();
            StringBuilder documento = new StringBuilder();

            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                String texto = extractor.getTextFromPage(pagina);
                paginas.add(texto);
                documento.append(texto).append('\n');
            }

            Integer anio = extraerAnio(documento.toString());
            if (anio == null) {
                throw archivoInvalido(
                        "No se pudo identificar el año de vigencia del PDF"
                );
            }

            List<SpringerApcRequestDTO> registros = new ArrayList<>();
            int rechazados = 0;

            for (String pagina : paginas) {
                ParseoPdf resultadoPagina = extraerRegistrosPagina(pagina, anio);
                registros.addAll(resultadoPagina.registros());
                rechazados += resultadoPagina.rechazados();
            }

            if (registros.isEmpty() && rechazados == 0) {
                throw archivoInvalido(
                        "El PDF no contiene filas APC reconocibles"
                );
            }

            return new ParseoPdf(List.copyOf(registros), rechazados);
        }
    }

    ParseoPdf extraerRegistrosPagina(String texto, Integer anio) {
        List<SpringerApcRequestDTO> registros = new ArrayList<>();
        StringBuilder acumulado = new StringBuilder();
        SpringerApcRequestDTO pendiente = null;
        String textoMinusculas = texto.toLowerCase(Locale.ROOT);
        boolean tablaDetectada = !textoMinusculas.contains("journal title")
                && !textoMinusculas.contains("journal name");
        boolean encabezadoDetectado = false;
        int rechazados = 0;

        for (String lineaOriginal : texto.split("\\R")) {
            String linea = normalizarLinea(lineaOriginal);
            if (linea == null) {
                continue;
            }

            String lineaMinusculas = linea.toLowerCase(Locale.ROOT);
            if (lineaMinusculas.contains("journal title")
                    || lineaMinusculas.contains("journal name")) {
                encabezadoDetectado = true;
                tablaDetectada = lineaMinusculas.contains("eissn");
                acumulado.setLength(0);
                continue;
            }

            if (encabezadoDetectado
                    && !tablaDetectada
                    && lineaMinusculas.contains("eissn")) {
                tablaDetectada = true;
                continue;
            }

            if (!tablaDetectada || esEncabezadoOPie(linea)) {
                continue;
            }

            if (pendiente != null) {
                if (URL_PATTERN.matcher(linea).find()
                        && !ISSN_PATTERN.matcher(linea).find()) {
                    pendiente.setUrlOficial(extraerUrl(linea));
                    registros.add(pendiente);
                    pendiente = null;
                    continue;
                }

                registros.add(pendiente);
                pendiente = null;
            }

            if (!acumulado.isEmpty()) {
                acumulado.append(' ');
            }
            acumulado.append(linea);

            if (!ISSN_PATTERN.matcher(acumulado).find()) {
                continue;
            }

            String fila = acumulado.toString();

            if (esPrecioCompuestoAmbiguo(fila)) {
                rechazados++;
                LOGGER.warn(
                        "Importación PDF Springer: fila con APC compuesto no interpretable: {}",
                        fila
                );
                acumulado.setLength(0);
                continue;
            }

            SpringerApcRequestDTO registro = interpretarFila(fila, anio);
            if (registro != null) {
                pendiente = registro;
                acumulado.setLength(0);
            }
        }

        if (pendiente != null) {
            registros.add(pendiente);
        } else if (ISSN_PATTERN.matcher(acumulado).find()) {
            rechazados++;
        }

        return new ParseoPdf(List.copyOf(registros), rechazados);
    }

    private SpringerApcRequestDTO interpretarFila(String fila, Integer anio) {
        Matcher issnMatcher = ISSN_PATTERN.matcher(fila);
        if (!issnMatcher.find()) {
            return null;
        }

        String prefijo = normalizarLinea(fila.substring(0, issnMatcher.start()));
        String resto = fila.substring(issnMatcher.end());
        String url = extraerUrl(resto);
        String restoSinUrl = URL_PATTERN.matcher(resto).replaceAll(" ");

        List<String> precios = new ArrayList<>();
        Matcher precioMatcher = APC_PATTERN.matcher(restoSinUrl);
        while (precioMatcher.find()) {
            precios.add(normalizarLinea(precioMatcher.group()));
        }

        if (precios.size() < 3) {
            return null;
        }

        TituloEImprint tituloEImprint = separarTituloEImprint(prefijo);
        if (tituloEImprint.titulo() == null) {
            return null;
        }

        SpringerApcRequestDTO registro = new SpringerApcRequestDTO();
        registro.setTitulo(tituloEImprint.titulo());
        registro.setImprint(tituloEImprint.imprint());
        registro.setEissn(issnMatcher.group());
        registro.setApcEur(precios.get(0));
        registro.setApcUsd(precios.get(1));
        registro.setApcGbp(precios.get(2));
        registro.setUrlOficial(url);
        registro.setAnioVigencia(anio);
        return registro;
    }

    private TituloEImprint separarTituloEImprint(String prefijo) {
        if (prefijo == null) {
            return new TituloEImprint(null, null);
        }

        String minusculas = prefijo.toLowerCase(Locale.ROOT);
        String imprintEncontrado = null;
        int posicion = -1;

        for (String imprint : IMPRINTS) {
            int candidato = minusculas.lastIndexOf(
                    imprint.toLowerCase(Locale.ROOT));
            if (candidato > posicion
                    && candidato + imprint.length() == prefijo.length()) {
                posicion = candidato;
                imprintEncontrado = imprint;
            }
        }

        if (imprintEncontrado == null) {
            return new TituloEImprint(prefijo, null);
        }

        return new TituloEImprint(
                normalizarLinea(prefijo.substring(0, posicion)),
                imprintEncontrado
        );
    }

    private Integer extraerAnio(String texto) {
        Matcher matcher = YEAR_PATTERN.matcher(texto);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private String extraerUrl(String texto) {
        Matcher matcher = URL_PATTERN.matcher(texto);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group()
                .replaceAll("[),.;]+$", "");
    }

    private boolean esEncabezadoOPie(String linea) {
        String valor = linea.toLowerCase(Locale.ROOT);
        return valor.contains("apcs correct as of")
                || valor.startsWith("springer nature fully")
                || valor.startsWith("springer nature hybrid")
                || valor.matches(".*\\b20\\d{2}\\s+eur\\b.*")
                || valor.equals("eur")
                || valor.equals("usd")
                || valor.equals("gbp")
                || valor.matches("^\\d+$");
    }

    private boolean esPrecioCompuestoAmbiguo(String fila) {
        String valor = fila.toLowerCase(Locale.ROOT);
        return valor.contains("mandatory") || valor.contains("oa apc");
    }

    private String normalizarLinea(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
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

    record ParseoPdf(
            List<SpringerApcRequestDTO> registros,
            int rechazados
    ) {
    }

    private record TituloEImprint(
            String titulo,
            String imprint
    ) {
    }
}
// FIN - Importación PDF Springer
