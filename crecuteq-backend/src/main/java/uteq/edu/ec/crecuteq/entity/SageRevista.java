package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sage_revista", uniqueConstraints =
        @UniqueConstraint(name = "uk_sage_modelo_codigo", columnNames = {"modelo_publicacion", "journal_code"}))
public class SageRevista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sage_revista") private Long id;
    @Column(nullable = false, length = 1000) private String titulo;
    @Column(name = "journal_code", nullable = false, length = 50) private String journalCode;
    @Column(length = 20) private String tla;
    @Column(length = 20) private String issn;
    @Column(name = "issn_normalizado", length = 8) private String issnNormalizado;
    @Column(length = 20) private String eissn;
    @Column(name = "eissn_normalizado", length = 8) private String eissnNormalizado;
    @Column(name = "modelo_publicacion", nullable = false, length = 50) private String modeloPublicacion;
    @Column(length = 50) private String division;
    @Column(name = "precio_lista", length = 50) private String precioLista;
    @Column(name = "precio_actual", length = 50) private String precioActual;
    @Column(length = 20) private String moneda;
    @Column(name = "apc_usd", length = 50) private String apcUsd;
    @Column(name = "apc_gbp", length = 50) private String apcGbp;
    @Column(name = "url_oficial", length = 2000) private String urlOficial;
    @Column(name = "fuente_archivo", length = 500) private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false) private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;

    public Long getId() { return id; }
    public String getTitulo() { return titulo; } public void setTitulo(String v) { titulo = v; }
    public String getJournalCode() { return journalCode; } public void setJournalCode(String v) { journalCode = v; }
    public String getTla() { return tla; } public void setTla(String v) { tla = v; }
    public String getIssn() { return issn; } public void setIssn(String v) { issn = v; }
    public String getIssnNormalizado() { return issnNormalizado; } public void setIssnNormalizado(String v) { issnNormalizado = v; }
    public String getEissn() { return eissn; } public void setEissn(String v) { eissn = v; }
    public String getEissnNormalizado() { return eissnNormalizado; } public void setEissnNormalizado(String v) { eissnNormalizado = v; }
    public String getModeloPublicacion() { return modeloPublicacion; } public void setModeloPublicacion(String v) { modeloPublicacion = v; }
    public String getDivision() { return division; } public void setDivision(String v) { division = v; }
    public String getPrecioLista() { return precioLista; } public void setPrecioLista(String v) { precioLista = v; }
    public String getPrecioActual() { return precioActual; } public void setPrecioActual(String v) { precioActual = v; }
    public String getMoneda() { return moneda; } public void setMoneda(String v) { moneda = v; }
    public String getApcUsd() { return apcUsd; } public void setApcUsd(String v) { apcUsd = v; }
    public String getApcGbp() { return apcGbp; } public void setApcGbp(String v) { apcGbp = v; }
    public String getUrlOficial() { return urlOficial; } public void setUrlOficial(String v) { urlOficial = v; }
    public String getFuenteArchivo() { return fuenteArchivo; } public void setFuenteArchivo(String v) { fuenteArchivo = v; }
    @PrePersist void prePersist() { fechaImportacion = LocalDateTime.now(); fechaActualizacion = fechaImportacion; }
    @PreUpdate void preUpdate() { fechaActualizacion = LocalDateTime.now(); }
}
