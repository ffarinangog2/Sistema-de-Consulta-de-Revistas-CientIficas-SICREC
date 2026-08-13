package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="degruyter_revista")
public class DegruyterRevista {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_degruyter_revista") private Long id;
    @Column(name="codigo_online",nullable=false,unique=true,length=80) private String codigoOnline;
    @Column(nullable=false,length=1000) private String titulo;
    @Column(length=20) private String issn; @Column(name="issn_normalizado",length=8) private String issnNormalizado;
    @Column(length=20) private String eissn; @Column(name="eissn_normalizado",length=8) private String eissnNormalizado;
    @Column(length=255) private String editorial; @Column(name="modelo_publicacion",length=100) private String modeloPublicacion;
    @Column(name="apc_eur",length=255) private String apcEur; @Column(name="area_tematica",length=500) private String areaTematica;
    @Column(length=200) private String idioma; @Column(length=500) private String licencia;
    @Column(name="url_oficial",length=2000) private String urlOficial; @Column(name="fuente_archivo",length=500) private String fuenteArchivo;
    @Column(name="fecha_importacion",nullable=false,updatable=false) private LocalDateTime fechaImportacion;
    @Column(name="fecha_actualizacion",nullable=false) private LocalDateTime fechaActualizacion;
    public Long getId(){return id;} public String getCodigoOnline(){return codigoOnline;} public void setCodigoOnline(String v){codigoOnline=v;} public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getIssn(){return issn;} public void setIssn(String v){issn=v;} public String getIssnNormalizado(){return issnNormalizado;} public void setIssnNormalizado(String v){issnNormalizado=v;}
    public String getEissn(){return eissn;} public void setEissn(String v){eissn=v;} public String getEissnNormalizado(){return eissnNormalizado;} public void setEissnNormalizado(String v){eissnNormalizado=v;}
    public String getEditorial(){return editorial;} public void setEditorial(String v){editorial=v;} public String getModeloPublicacion(){return modeloPublicacion;} public void setModeloPublicacion(String v){modeloPublicacion=v;}
    public String getApcEur(){return apcEur;} public void setApcEur(String v){apcEur=v;} public String getAreaTematica(){return areaTematica;} public void setAreaTematica(String v){areaTematica=v;}
    public String getIdioma(){return idioma;} public void setIdioma(String v){idioma=v;} public String getLicencia(){return licencia;} public void setLicencia(String v){licencia=v;}
    public String getUrlOficial(){return urlOficial;} public void setUrlOficial(String v){urlOficial=v;} public String getFuenteArchivo(){return fuenteArchivo;} public void setFuenteArchivo(String v){fuenteArchivo=v;}
    @PrePersist void prePersist(){fechaImportacion=LocalDateTime.now();fechaActualizacion=fechaImportacion;} @PreUpdate void preUpdate(){fechaActualizacion=LocalDateTime.now();}
}
