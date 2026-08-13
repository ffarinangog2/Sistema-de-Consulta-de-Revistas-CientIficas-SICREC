package uteq.edu.ec.crecuteq.dto;

import java.time.LocalDateTime;

// INICIO - Módulo Springer
public class SpringerRevistaResponseDTO {

    private Long id;
    private String titulo;
    private String issn;
    private String issnNormalizado;
    private String eissn;
    private String eissnNormalizado;
    private String productId;
    private String imprint;
    private String publishingModel;
    private String hybridType;
    private String primaryLanguage;
    private String urlOficial;
    private String fuenteArchivo;
    // INICIO - Importación PDF Springer
    private String apcEur;
    private String apcUsd;
    private String apcGbp;
    private String apcWebsite;
    private Integer anioVigenciaApc;
    // FIN - Importación PDF Springer
    private LocalDateTime fechaImportacion;
    private LocalDateTime fechaActualizacion;

    public SpringerRevistaResponseDTO() {
    }

    public SpringerRevistaResponseDTO(
            Long id, String titulo, String issn, String issnNormalizado,
            String eissn, String eissnNormalizado, String productId,
            String imprint, String publishingModel, String hybridType,
            String primaryLanguage, String urlOficial, String fuenteArchivo,
            String apcEur, String apcUsd, String apcGbp, String apcWebsite,
            Integer anioVigenciaApc,
            LocalDateTime fechaImportacion, LocalDateTime fechaActualizacion
    ) {
        this.id = id;
        this.titulo = titulo;
        this.issn = issn;
        this.issnNormalizado = issnNormalizado;
        this.eissn = eissn;
        this.eissnNormalizado = eissnNormalizado;
        this.productId = productId;
        this.imprint = imprint;
        this.publishingModel = publishingModel;
        this.hybridType = hybridType;
        this.primaryLanguage = primaryLanguage;
        this.urlOficial = urlOficial;
        this.fuenteArchivo = fuenteArchivo;
        this.apcEur = apcEur;
        this.apcUsd = apcUsd;
        this.apcGbp = apcGbp;
        this.apcWebsite = apcWebsite;
        this.anioVigenciaApc = anioVigenciaApc;
        this.fechaImportacion = fechaImportacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getIssn() { return issn; }
    public String getIssnNormalizado() { return issnNormalizado; }
    public String getEissn() { return eissn; }
    public String getEissnNormalizado() { return eissnNormalizado; }
    public String getProductId() { return productId; }
    public String getImprint() { return imprint; }
    public String getPublishingModel() { return publishingModel; }
    public String getHybridType() { return hybridType; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public String getUrlOficial() { return urlOficial; }
    public String getFuenteArchivo() { return fuenteArchivo; }
    // INICIO - Importación PDF Springer
    public String getApcEur() { return apcEur; }
    public String getApcUsd() { return apcUsd; }
    public String getApcGbp() { return apcGbp; }
    public String getApcWebsite() { return apcWebsite; }
    public Integer getAnioVigenciaApc() { return anioVigenciaApc; }
    // FIN - Importación PDF Springer
    public LocalDateTime getFechaImportacion() { return fechaImportacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
// FIN - Módulo Springer
