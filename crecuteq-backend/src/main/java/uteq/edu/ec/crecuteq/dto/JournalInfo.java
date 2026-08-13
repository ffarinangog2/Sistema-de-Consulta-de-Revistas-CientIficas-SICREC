package uteq.edu.ec.crecuteq.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// INICIO - Implementación común de proveedores de revistas
/**
 * Resultado neutral de una consulta a cualquier proveedor editorial.
 * Los campos no normalizados se conservan en {@code datosAdicionales}.
 */
public class JournalInfo {
    private final String proveedor;
    private final String titulo;
    private final String issn;
    private final String eissn;
    private final String editorial;
    private final String pais;
    private final String idiomas;
    private final String materias;
    private final String licencia;
    private final String apc;
    private final String monedaApc;
    private final String url;
    private final Map<String, String> datosAdicionales;

    public JournalInfo(
            String proveedor, String titulo, String issn, String eissn,
            String editorial, String pais, String idiomas, String materias,
            String licencia, String apc, String monedaApc, String url,
            Map<String, String> datosAdicionales
    ) {
        this.proveedor = proveedor;
        this.titulo = titulo;
        this.issn = issn;
        this.eissn = eissn;
        this.editorial = editorial;
        this.pais = pais;
        this.idiomas = idiomas;
        this.materias = materias;
        this.licencia = licencia;
        this.apc = apc;
        this.monedaApc = monedaApc;
        this.url = url;
        this.datosAdicionales = Collections.unmodifiableMap(
                new LinkedHashMap<>(datosAdicionales == null ? Map.of() : datosAdicionales)
        );
    }

    public String getProveedor() { return proveedor; }
    public String getTitulo() { return titulo; }
    public String getIssn() { return issn; }
    public String getEissn() { return eissn; }
    public String getEditorial() { return editorial; }
    public String getPais() { return pais; }
    public String getIdiomas() { return idiomas; }
    public String getMaterias() { return materias; }
    public String getLicencia() { return licencia; }
    public String getApc() { return apc; }
    public String getMonedaApc() { return monedaApc; }
    public String getUrl() { return url; }
    public Map<String, String> getDatosAdicionales() { return datosAdicionales; }
}
// FIN - Implementación común de proveedores de revistas
