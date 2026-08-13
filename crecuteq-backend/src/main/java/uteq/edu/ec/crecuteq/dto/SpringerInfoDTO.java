package uteq.edu.ec.crecuteq.dto;

// INICIO - Integración Springer en búsquedas
public class SpringerInfoDTO {
    private String titulo;
    private String issn;
    private String eissn;
    private String imprint;
    private String modeloPublicacion;
    private String tipoHibrido;
    private String idioma;
    private Integer cantidadVolumenes;
    private Integer numerosProgramados;
    private String comentarios;
    private String urlOficial;
    private String apcEur;
    private String apcUsd;
    private String apcGbp;
    private String apcWebsite;
    private String periodicidadEstimada;
    private String estadoSpringer;
    private Integer numerosPorVolumen;

    public SpringerInfoDTO() {
    }

    public SpringerInfoDTO(
            String titulo, String issn, String eissn, String imprint,
            String modeloPublicacion, String tipoHibrido, String idioma,
            Integer cantidadVolumenes, Integer numerosProgramados,
            String comentarios, String urlOficial, String apcEur,
            String apcUsd, String apcGbp, String apcWebsite,
            String periodicidadEstimada, String estadoSpringer,
            Integer numerosPorVolumen
    ) {
        this.titulo = titulo;
        this.issn = issn;
        this.eissn = eissn;
        this.imprint = imprint;
        this.modeloPublicacion = modeloPublicacion;
        this.tipoHibrido = tipoHibrido;
        this.idioma = idioma;
        this.cantidadVolumenes = cantidadVolumenes;
        this.numerosProgramados = numerosProgramados;
        this.comentarios = comentarios;
        this.urlOficial = urlOficial;
        this.apcEur = apcEur;
        this.apcUsd = apcUsd;
        this.apcGbp = apcGbp;
        this.apcWebsite = apcWebsite;
        this.periodicidadEstimada = periodicidadEstimada;
        this.estadoSpringer = estadoSpringer;
        this.numerosPorVolumen = numerosPorVolumen;
    }

    public String getTitulo() { return titulo; }
    public String getIssn() { return issn; }
    public String getEissn() { return eissn; }
    public String getImprint() { return imprint; }
    public String getModeloPublicacion() { return modeloPublicacion; }
    public String getTipoHibrido() { return tipoHibrido; }
    public String getIdioma() { return idioma; }
    public Integer getCantidadVolumenes() { return cantidadVolumenes; }
    public Integer getNumerosProgramados() { return numerosProgramados; }
    public String getComentarios() { return comentarios; }
    public String getUrlOficial() { return urlOficial; }
    public String getApcEur() { return apcEur; }
    public String getApcUsd() { return apcUsd; }
    public String getApcGbp() { return apcGbp; }
    public String getApcWebsite() { return apcWebsite; }
    public String getPeriodicidadEstimada() { return periodicidadEstimada; }
    public String getEstadoSpringer() { return estadoSpringer; }
    public Integer getNumerosPorVolumen() { return numerosPorVolumen; }
}
// FIN - Integración Springer en búsquedas
