package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Precio APC publicado en la lista oficial de Elsevier. */
@Entity
@Table(name = "elsevier_revista", indexes = @Index(name = "idx_elsevier_issn_normalizado", columnList = "issn_normalizado"))
public class ElsevierRevista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_elsevier_revista") private Long id;
    @Column(nullable = false, length = 20) private String issn;
    @Column(name = "issn_normalizado", nullable = false, unique = true, length = 8) private String issnNormalizado;
    @Column(nullable = false, length = 1000) private String titulo;
    @Column(name = "modelo_publicacion", length = 100) private String modeloPublicacion;
    @Column(name = "apc_usd", length = 50) private String apcUsd;
    @Column(name = "apc_eur", length = 50) private String apcEur;
    @Column(name = "apc_gbp", length = 50) private String apcGbp;
    @Column(name = "apc_jpy", length = 50) private String apcJpy;
    @Column(name = "vigencia", length = 100) private String vigencia;
    @Column(name = "fuente_archivo", length = 500) private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false) private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;

    public Long getId() { return id; }
    public String getIssn() { return issn; }
    public void setIssn(String valor) { issn = valor; }
    public String getIssnNormalizado() { return issnNormalizado; }
    public void setIssnNormalizado(String valor) { issnNormalizado = valor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String valor) { titulo = valor; }
    public String getModeloPublicacion() { return modeloPublicacion; }
    public void setModeloPublicacion(String valor) { modeloPublicacion = valor; }
    public String getApcUsd() { return apcUsd; }
    public void setApcUsd(String valor) { apcUsd = valor; }
    public String getApcEur() { return apcEur; }
    public void setApcEur(String valor) { apcEur = valor; }
    public String getApcGbp() { return apcGbp; }
    public void setApcGbp(String valor) { apcGbp = valor; }
    public String getApcJpy() { return apcJpy; }
    public void setApcJpy(String valor) { apcJpy = valor; }
    public String getVigencia() { return vigencia; }
    public void setVigencia(String valor) { vigencia = valor; }
    public String getFuenteArchivo() { return fuenteArchivo; }
    public void setFuenteArchivo(String valor) { fuenteArchivo = valor; }

    @PrePersist void prePersist() { fechaImportacion = LocalDateTime.now(); fechaActualizacion = fechaImportacion; }
    @PreUpdate void preUpdate() { fechaActualizacion = LocalDateTime.now(); }
}
