package uteq.edu.ec.crecuteq.dto;

import java.time.LocalDateTime;

public class FavoritoDTO {

    private String titulo;

    private String revista;

    private String cuartil;

    private Integer anio;

    private LocalDateTime fechaGuardado;

    public FavoritoDTO() {
    }

    public FavoritoDTO(
            String titulo,
            String revista,
            String cuartil,
            Integer anio,
            LocalDateTime fechaGuardado
    ) {

        this.titulo = titulo;
        this.revista = revista;
        this.cuartil = cuartil;
        this.anio = anio;
        this.fechaGuardado = fechaGuardado;

    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getRevista() {
        return revista;
    }

    public void setRevista(String revista) {
        this.revista = revista;
    }

    public String getCuartil() {
        return cuartil;
    }

    public void setCuartil(String cuartil) {
        this.cuartil = cuartil;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public LocalDateTime getFechaGuardado() {
        return fechaGuardado;
    }

    public void setFechaGuardado(LocalDateTime fechaGuardado) {
        this.fechaGuardado = fechaGuardado;
    }

}