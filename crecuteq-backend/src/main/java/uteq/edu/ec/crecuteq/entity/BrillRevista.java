package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "brill_revista")
public class BrillRevista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_brill_revista") private Long id;
    @Column(name = "eissn_normalizado", nullable = false, unique = true, length = 8) private String eissnNormalizado;
    @Column(nullable = false, length = 20) private String eissn;
    @Column(nullable = false, length = 1000) private String titulo;
    @Column(length = 100) private String editorial;
    @Column(name = "apc_eur", length = 50) private String apcEur;
    @Column(name = "tipo_oa", length = 100) private String tipoOa;
    @Column(name = "area_tematica", length = 500) private String areaTematica;
    @Column(length = 500) private String subdisciplina;
    @Column(name = "factor_impacto", length = 50) private String factorImpacto;
    @Column(length = 200) private String idioma;
    @Column(name = "url_oficial", length = 2000) private String urlOficial;
    @Column(name = "fuente_archivo", length = 500) private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false) private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;
    public Long getId(){return id;} public String getEissnNormalizado(){return eissnNormalizado;} public void setEissnNormalizado(String v){eissnNormalizado=v;}
    public String getEissn(){return eissn;} public void setEissn(String v){eissn=v;} public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getEditorial(){return editorial;} public void setEditorial(String v){editorial=v;} public String getApcEur(){return apcEur;} public void setApcEur(String v){apcEur=v;}
    public String getTipoOa(){return tipoOa;} public void setTipoOa(String v){tipoOa=v;} public String getAreaTematica(){return areaTematica;} public void setAreaTematica(String v){areaTematica=v;}
    public String getSubdisciplina(){return subdisciplina;} public void setSubdisciplina(String v){subdisciplina=v;} public String getFactorImpacto(){return factorImpacto;} public void setFactorImpacto(String v){factorImpacto=v;}
    public String getIdioma(){return idioma;} public void setIdioma(String v){idioma=v;} public String getUrlOficial(){return urlOficial;} public void setUrlOficial(String v){urlOficial=v;}
    public String getFuenteArchivo(){return fuenteArchivo;} public void setFuenteArchivo(String v){fuenteArchivo=v;}
    @PrePersist void prePersist(){fechaImportacion=LocalDateTime.now();fechaActualizacion=fechaImportacion;} @PreUpdate void preUpdate(){fechaActualizacion=LocalDateTime.now();}
}
