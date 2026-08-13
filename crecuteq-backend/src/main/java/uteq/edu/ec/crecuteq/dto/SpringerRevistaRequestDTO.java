package uteq.edu.ec.crecuteq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// INICIO - Módulo Springer
public class SpringerRevistaRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 500, message = "El título no puede superar 500 caracteres")
    private String titulo;

    @Size(max = 20, message = "El ISSN no puede superar 20 caracteres")
    private String issn;

    @Size(max = 20, message = "El eISSN no puede superar 20 caracteres")
    private String eissn;

    @Size(max = 50, message = "El Product ID no puede superar 50 caracteres")
    private String productId;

    @Size(max = 255, message = "El imprint no puede superar 255 caracteres")
    private String imprint;

    @Size(max = 100, message = "El modelo de publicación no puede superar 100 caracteres")
    private String publishingModel;

    @Size(max = 100, message = "El tipo híbrido no puede superar 100 caracteres")
    private String hybridType;

    @Size(max = 100, message = "El idioma principal no puede superar 100 caracteres")
    private String primaryLanguage;

    @Size(max = 1000, message = "La URL oficial no puede superar 1000 caracteres")
    private String urlOficial;

    @Size(max = 500, message = "La fuente del archivo no puede superar 500 caracteres")
    private String fuenteArchivo;

    private Integer numerosPorVolumen;

    private Integer numerosProgramados;

    @Size(max = 2000, message = "Los comentarios no pueden superar 2000 caracteres")
    private String comentarios;

    public SpringerRevistaRequestDTO() {
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getEissn() { return eissn; }
    public void setEissn(String eissn) { this.eissn = eissn; }
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
}
// FIN - Módulo Springer
