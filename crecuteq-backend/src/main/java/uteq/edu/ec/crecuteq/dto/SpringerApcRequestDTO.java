package uteq.edu.ec.crecuteq.dto;

// INICIO - Importación PDF Springer
public class SpringerApcRequestDTO {

    private String productId;
    private String titulo;
    private String issn;
    private String eissn;
    private String imprint;
    private String apcEur;
    private String apcUsd;
    private String apcGbp;
    private String urlOficial;
    private Integer anioVigencia;

    public SpringerApcRequestDTO() {
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getEissn() { return eissn; }
    public void setEissn(String eissn) { this.eissn = eissn; }
    public String getImprint() { return imprint; }
    public void setImprint(String imprint) { this.imprint = imprint; }
    public String getApcEur() { return apcEur; }
    public void setApcEur(String apcEur) { this.apcEur = apcEur; }
    public String getApcUsd() { return apcUsd; }
    public void setApcUsd(String apcUsd) { this.apcUsd = apcUsd; }
    public String getApcGbp() { return apcGbp; }
    public void setApcGbp(String apcGbp) { this.apcGbp = apcGbp; }
    public String getUrlOficial() { return urlOficial; }
    public void setUrlOficial(String urlOficial) { this.urlOficial = urlOficial; }
    public Integer getAnioVigencia() { return anioVigencia; }
    public void setAnioVigencia(Integer anioVigencia) { this.anioVigencia = anioVigencia; }
}
// FIN - Importación PDF Springer
