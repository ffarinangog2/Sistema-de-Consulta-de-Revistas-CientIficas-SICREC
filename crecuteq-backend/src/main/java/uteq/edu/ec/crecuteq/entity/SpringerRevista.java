package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// INICIO - Módulo Springer
@Entity
@Table(
        name = "springer_revista",
        indexes = {
                @Index(name = "idx_springer_revista_issn_normalizado", columnList = "issn_normalizado"),
                @Index(name = "idx_springer_revista_eissn_normalizado", columnList = "eissn_normalizado"),
                @Index(name = "idx_springer_revista_product_id", columnList = "product_id")
        }
)
public class SpringerRevista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_springer_revista")
    private Long id;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(length = 20)
    private String issn;

    @Column(name = "issn_normalizado", length = 8)
    private String issnNormalizado;

    @Column(length = 20)
    private String eissn;

    @Column(name = "eissn_normalizado", length = 8)
    private String eissnNormalizado;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(length = 255)
    private String imprint;

    @Column(name = "modelo_publicacion", length = 100)
    private String publishingModel;

    @Column(name = "tipo_hibrido", length = 100)
    private String hybridType;

    @Column(name = "idioma_principal", length = 100)
    private String primaryLanguage;

    @Column(name = "url_oficial", length = 1000)
    private String urlOficial;

    @Column(name = "fuente_archivo", length = 500)
    private String fuenteArchivo;

    @Column(name = "numeros_por_volumen")
    private Integer numerosPorVolumen;

    @Column(name = "numeros_programados")
    private Integer numerosProgramados;

    @Column(length = 2000)
    private String comentarios;

    // INICIO - Importación PDF Springer
    @Column(name = "apc_eur", length = 255)
    private String apcEur;

    @Column(name = "apc_usd", length = 255)
    private String apcUsd;

    @Column(name = "apc_gbp", length = 255)
    private String apcGbp;

    @Column(name = "apc_website", length = 1000)
    private String apcWebsite;

    @Column(name = "anio_vigencia_apc")
    private Integer anioVigenciaApc;
    // FIN - Importación PDF Springer

    @Column(name = "fecha_importacion", nullable = false, updatable = false)
    private LocalDateTime fechaImportacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public SpringerRevista() {
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getIssnNormalizado() { return issnNormalizado; }
    public void setIssnNormalizado(String issnNormalizado) { this.issnNormalizado = issnNormalizado; }
    public String getEissn() { return eissn; }
    public void setEissn(String eissn) { this.eissn = eissn; }
    public String getEissnNormalizado() { return eissnNormalizado; }
    public void setEissnNormalizado(String eissnNormalizado) { this.eissnNormalizado = eissnNormalizado; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getImprint() { return imprint; }
    public void setImprint(String imprint) { this.imprint = imprint; }
    public String getPublishingModel() { return publishingModel; }
    public void setPublishingModel(String publishingModel) { this.publishingModel = publishingModel; }
    public String getHybridType() { return hybridType; }
    public void setHybridType(String hybridType) { this.hybridType = hybridType; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }
    public String getUrlOficial() { return urlOficial; }
    public void setUrlOficial(String urlOficial) { this.urlOficial = urlOficial; }
    public String getFuenteArchivo() { return fuenteArchivo; }
    public void setFuenteArchivo(String fuenteArchivo) { this.fuenteArchivo = fuenteArchivo; }
    public Integer getNumerosPorVolumen() { return numerosPorVolumen; }
    public void setNumerosPorVolumen(Integer numerosPorVolumen) { this.numerosPorVolumen = numerosPorVolumen; }
    public Integer getNumerosProgramados() { return numerosProgramados; }
    public void setNumerosProgramados(Integer numerosProgramados) { this.numerosProgramados = numerosProgramados; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    // INICIO - Importación PDF Springer
    public String getApcEur() { return apcEur; }
    public void setApcEur(String apcEur) { this.apcEur = apcEur; }
    public String getApcUsd() { return apcUsd; }
    public void setApcUsd(String apcUsd) { this.apcUsd = apcUsd; }
    public String getApcGbp() { return apcGbp; }
    public void setApcGbp(String apcGbp) { this.apcGbp = apcGbp; }
    public String getApcWebsite() { return apcWebsite; }
    public void setApcWebsite(String apcWebsite) { this.apcWebsite = apcWebsite; }
    public Integer getAnioVigenciaApc() { return anioVigenciaApc; }
    public void setAnioVigenciaApc(Integer anioVigenciaApc) { this.anioVigenciaApc = anioVigenciaApc; }
    // FIN - Importación PDF Springer
    public LocalDateTime getFechaImportacion() { return fechaImportacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaImportacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
// FIN - Módulo Springer
