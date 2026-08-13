package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ieee_revista")
public class IeeeRevista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ieee_revista") private Long id;
    @Column(nullable = false, unique = true, length = 40) private String acronimo;
    @Column(nullable = false, length = 1000) private String titulo;
    @Column(length = 20) private String issn;
    @Column(name = "issn_normalizado", length = 8) private String issnNormalizado;
    @Column(length = 20) private String eissn;
    @Column(name = "eissn_normalizado", length = 8) private String eissnNormalizado;
    @Column(name = "tipo_acceso", length = 80) private String tipoAcceso;
    @Column(name = "apc_usd", length = 50) private String apcUsd;
    @Column(name = "cargo_sobreextension", length = 100) private String cargoSobreextension;
    @Column(name = "tarifa_licencia_repositorio", length = 50) private String tarifaLicenciaRepositorio;
    @Column(name = "url_oficial", length = 2000) private String urlOficial;
    @Column(name = "fuente_archivo", length = 500) private String fuenteArchivo;
    @Column(name = "fecha_importacion", nullable = false, updatable = false) private LocalDateTime fechaImportacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;

    public Long getId(){return id;} public String getAcronimo(){return acronimo;} public void setAcronimo(String v){acronimo=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;} public String getIssn(){return issn;} public void setIssn(String v){issn=v;}
    public String getIssnNormalizado(){return issnNormalizado;} public void setIssnNormalizado(String v){issnNormalizado=v;} public String getEissn(){return eissn;} public void setEissn(String v){eissn=v;}
    public String getEissnNormalizado(){return eissnNormalizado;} public void setEissnNormalizado(String v){eissnNormalizado=v;} public String getTipoAcceso(){return tipoAcceso;} public void setTipoAcceso(String v){tipoAcceso=v;}
    public String getApcUsd(){return apcUsd;} public void setApcUsd(String v){apcUsd=v;} public String getCargoSobreextension(){return cargoSobreextension;} public void setCargoSobreextension(String v){cargoSobreextension=v;}
    public String getTarifaLicenciaRepositorio(){return tarifaLicenciaRepositorio;} public void setTarifaLicenciaRepositorio(String v){tarifaLicenciaRepositorio=v;} public String getUrlOficial(){return urlOficial;} public void setUrlOficial(String v){urlOficial=v;}
    public String getFuenteArchivo(){return fuenteArchivo;} public void setFuenteArchivo(String v){fuenteArchivo=v;}
    @PrePersist void prePersist(){fechaImportacion=LocalDateTime.now();fechaActualizacion=fechaImportacion;} @PreUpdate void preUpdate(){fechaActualizacion=LocalDateTime.now();}
}
