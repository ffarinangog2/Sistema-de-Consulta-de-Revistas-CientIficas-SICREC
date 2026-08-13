package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wiley_revista", indexes = @Index(name = "idx_wiley_online_issn", columnList = "online_issn_normalizado"))
public class WileyRevista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_wiley_revista") private Long id;
    @Column(nullable = false, length = 1000) private String titulo;
    @Column(name = "online_issn", nullable = false, length = 20) private String onlineIssn;
    @Column(name = "online_issn_normalizado", nullable = false, unique = true, length = 8) private String onlineIssnNormalizado;
    @Column(name = "area_tematica", length = 1000) private String areaTematica;
    @Column(name = "licencias", length = 1000) private String licencias;
    @Column(name = "modelo_publicacion", nullable = false, length = 100) private String modeloPublicacion;
    @Column(name = "apc_usd", length = 50) private String apcUsd;
    @Column(name = "apc_gbp", length = 50) private String apcGbp;
    @Column(name = "apc_eur", length = 50) private String apcEur;
    @Column(name = "vigencia", length = 100) private String vigencia;
    @Column(name = "fuente_archivo", length = 500) private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false) private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;

    public Long getId() { return id; }
    public String getTitulo() { return titulo; } public void setTitulo(String v) { titulo = v; }
    public String getOnlineIssn() { return onlineIssn; } public void setOnlineIssn(String v) { onlineIssn = v; }
    public String getOnlineIssnNormalizado() { return onlineIssnNormalizado; } public void setOnlineIssnNormalizado(String v) { onlineIssnNormalizado = v; }
    public String getAreaTematica() { return areaTematica; } public void setAreaTematica(String v) { areaTematica = v; }
    public String getLicencias() { return licencias; } public void setLicencias(String v) { licencias = v; }
    public String getModeloPublicacion() { return modeloPublicacion; } public void setModeloPublicacion(String v) { modeloPublicacion = v; }
    public String getApcUsd() { return apcUsd; } public void setApcUsd(String v) { apcUsd = v; }
    public String getApcGbp() { return apcGbp; } public void setApcGbp(String v) { apcGbp = v; }
    public String getApcEur() { return apcEur; } public void setApcEur(String v) { apcEur = v; }
    public String getVigencia() { return vigencia; } public void setVigencia(String v) { vigencia = v; }
    public String getFuenteArchivo() { return fuenteArchivo; } public void setFuenteArchivo(String v) { fuenteArchivo = v; }
    @PrePersist void prePersist() { fechaImportacion = LocalDateTime.now(); fechaActualizacion = fechaImportacion; }
    @PreUpdate void preUpdate() { fechaActualizacion = LocalDateTime.now(); }
}
