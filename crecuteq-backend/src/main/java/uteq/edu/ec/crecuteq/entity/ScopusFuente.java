package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Datos permitidos del Source Title List; la bibliografía pertenece a la API. */
@Entity
@Table(name = "scopus_fuente", indexes = {
        @Index(name = "idx_scopus_fuente_issn", columnList = "issn_normalizado"),
        @Index(name = "idx_scopus_fuente_eissn", columnList = "eissn_normalizado")
})
public class ScopusFuente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_scopus_fuente")
    private Long id;
    @Column(length = 20) private String issn;
    @Column(name = "issn_normalizado", length = 8) private String issnNormalizado;
    @Column(length = 20) private String eissn;
    @Column(name = "eissn_normalizado", length = 8) private String eissnNormalizado;
    @Column(length = 100) private String estado;
    @Column(nullable = false) private boolean discontinuada;
    @Column(length = 255) private String periodicidad;
    @Column(name = "fecha_importacion", nullable = false, updatable = false)
    private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getIssnNormalizado() { return issnNormalizado; }
    public void setIssnNormalizado(String valor) { this.issnNormalizado = valor; }
    public String getEissn() { return eissn; }
    public void setEissn(String eissn) { this.eissn = eissn; }
    public String getEissnNormalizado() { return eissnNormalizado; }
    public void setEissnNormalizado(String valor) { this.eissnNormalizado = valor; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isDiscontinuada() { return discontinuada; }
    public void setDiscontinuada(boolean discontinuada) { this.discontinuada = discontinuada; }
    public String getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(String periodicidad) { this.periodicidad = periodicidad; }

    @PrePersist
    void prePersist() {
        fechaImportacion = LocalDateTime.now();
        fechaActualizacion = fechaImportacion;
    }
    @PreUpdate
    void preUpdate() { fechaActualizacion = LocalDateTime.now(); }
}
