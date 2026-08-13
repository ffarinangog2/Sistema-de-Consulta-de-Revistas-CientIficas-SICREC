package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// INICIO - Persistencia del catálogo oficial DOAJ
@Entity
@Table(name = "doaj_revista", indexes = {
        @Index(name = "idx_doaj_issn_normalizado", columnList = "issn_normalizado"),
        @Index(name = "idx_doaj_eissn_normalizado", columnList = "eissn_normalizado")
})
public class DoajRevista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doaj_revista")
    private Long id;

    @Column(nullable = false, length = 1000)
    private String titulo;
    @Column(length = 20)
    private String issn;
    @Column(name = "issn_normalizado", length = 8)
    private String issnNormalizado;
    @Column(length = 20)
    private String eissn;
    @Column(name = "eissn_normalizado", length = 8)
    private String eissnNormalizado;
    @Column(length = 1000)
    private String editorial;
    @Column(length = 255)
    private String pais;
    @Column(length = 2000)
    private String idiomas;
    @Column(length = 4000)
    private String materias;
    @Column(length = 1000)
    private String licencia;
    @Column(length = 255)
    private String apc;
    @Column(name = "moneda_apc", length = 50)
    private String monedaApc;
    @Column(name = "url_oficial", length = 2000)
    private String urlOficial;
    @Lob
    @Column(name = "datos_csv", nullable = false, columnDefinition = "TEXT")
    private String datosCsv;
    @Column(name = "fuente_archivo", length = 500)
    private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false)
    private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

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
    public String getEditorial() { return editorial; }
    public void setEditorial(String editorial) { this.editorial = editorial; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getIdiomas() { return idiomas; }
    public void setIdiomas(String idiomas) { this.idiomas = idiomas; }
    public String getMaterias() { return materias; }
    public void setMaterias(String materias) { this.materias = materias; }
    public String getLicencia() { return licencia; }
    public void setLicencia(String licencia) { this.licencia = licencia; }
    public String getApc() { return apc; }
    public void setApc(String apc) { this.apc = apc; }
    public String getMonedaApc() { return monedaApc; }
    public void setMonedaApc(String monedaApc) { this.monedaApc = monedaApc; }
    public String getUrlOficial() { return urlOficial; }
    public void setUrlOficial(String urlOficial) { this.urlOficial = urlOficial; }
    public String getDatosCsv() { return datosCsv; }
    public void setDatosCsv(String datosCsv) { this.datosCsv = datosCsv; }
    public String getFuenteArchivo() { return fuenteArchivo; }
    public void setFuenteArchivo(String fuenteArchivo) { this.fuenteArchivo = fuenteArchivo; }

    @PrePersist
    void prePersist() {
        fechaImportacion = LocalDateTime.now();
        fechaActualizacion = fechaImportacion;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
// FIN - Persistencia del catálogo oficial DOAJ
