package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="cambridge_revista")
public class CambridgeRevista {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_cambridge_revista") private Long id;
    @Column(nullable=false,unique=true,length=30) private String mnemonic;
    @Column(nullable=false,length=1000) private String titulo;
    @Column(length=20) private String issn;
    @Column(name="issn_normalizado",length=8) private String issnNormalizado;
    @Column(length=20) private String eissn;
    @Column(name="eissn_normalizado",length=8) private String eissnNormalizado;
    @Column(length=20) private String area;
    @Column(name="modelo_publicacion",length=100) private String modeloPublicacion;
    @Column(name="apc_gbp",length=50) private String apcGbp;
    @Column(name="apc_gbp_miembro",length=50) private String apcGbpMiembro;
    @Column(name="apc_usd",length=50) private String apcUsd;
    @Column(name="apc_usd_miembro",length=50) private String apcUsdMiembro;
    @Column(name="apc_eur",length=50) private String apcEur;
    @Column(name="apc_eur_miembro",length=50) private String apcEurMiembro;
    @Column(name="apc_aud",length=50) private String apcAud;
    @Column(name="apc_aud_miembro",length=50) private String apcAudMiembro;
    @Column(name="notas_apc",length=2000) private String notasApc;
    @Column(length=1000) private String licencias;
    @Column(name="url_oficial",length=2000) private String urlOficial;
    @Column(name="fuente_archivo",length=500) private String fuenteArchivo;
    @Column(name="fecha_importacion",nullable=false,updatable=false) private LocalDateTime fechaImportacion;
    @Column(name="fecha_actualizacion",nullable=false) private LocalDateTime fechaActualizacion;
    public Long getId(){return id;} public String getMnemonic(){return mnemonic;} public void setMnemonic(String v){mnemonic=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;} public String getIssn(){return issn;} public void setIssn(String v){issn=v;}
    public String getIssnNormalizado(){return issnNormalizado;} public void setIssnNormalizado(String v){issnNormalizado=v;} public String getEissn(){return eissn;} public void setEissn(String v){eissn=v;}
    public String getEissnNormalizado(){return eissnNormalizado;} public void setEissnNormalizado(String v){eissnNormalizado=v;} public String getArea(){return area;} public void setArea(String v){area=v;}
    public String getModeloPublicacion(){return modeloPublicacion;} public void setModeloPublicacion(String v){modeloPublicacion=v;} public String getApcGbp(){return apcGbp;} public void setApcGbp(String v){apcGbp=v;}
    public String getApcGbpMiembro(){return apcGbpMiembro;} public void setApcGbpMiembro(String v){apcGbpMiembro=v;} public String getApcUsd(){return apcUsd;} public void setApcUsd(String v){apcUsd=v;}
    public String getApcUsdMiembro(){return apcUsdMiembro;} public void setApcUsdMiembro(String v){apcUsdMiembro=v;} public String getApcEur(){return apcEur;} public void setApcEur(String v){apcEur=v;}
    public String getApcEurMiembro(){return apcEurMiembro;} public void setApcEurMiembro(String v){apcEurMiembro=v;} public String getApcAud(){return apcAud;} public void setApcAud(String v){apcAud=v;}
    public String getApcAudMiembro(){return apcAudMiembro;} public void setApcAudMiembro(String v){apcAudMiembro=v;} public String getNotasApc(){return notasApc;} public void setNotasApc(String v){notasApc=v;}
    public String getLicencias(){return licencias;} public void setLicencias(String v){licencias=v;} public String getUrlOficial(){return urlOficial;} public void setUrlOficial(String v){urlOficial=v;}
    public String getFuenteArchivo(){return fuenteArchivo;} public void setFuenteArchivo(String v){fuenteArchivo=v;}
    @PrePersist void prePersist(){fechaImportacion=LocalDateTime.now();fechaActualizacion=fechaImportacion;} @PreUpdate void preUpdate(){fechaActualizacion=LocalDateTime.now();}
}
