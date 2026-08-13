package uteq.edu.ec.crecuteq.dto;

import uteq.edu.ec.crecuteq.entity.HistorialBusqueda;

import java.util.List;

public class DashboardUsuarioDTO {

    private Long totalBusquedas;

    private Long totalFavoritos;

    private List<UltimaBusquedaDTO> ultimasBusquedas;

    private List<FavoritoDTO> ultimosFavoritos;

    public DashboardUsuarioDTO() {
    }

    public Long getTotalBusquedas() {
        return totalBusquedas;
    }

    public void setTotalBusquedas(Long totalBusquedas) {
        this.totalBusquedas = totalBusquedas;
    }

    public Long getTotalFavoritos() {
        return totalFavoritos;
    }

    public void setTotalFavoritos(Long totalFavoritos) {
        this.totalFavoritos = totalFavoritos;
    }

    public List<UltimaBusquedaDTO> getUltimasBusquedas() {
        return ultimasBusquedas;
    }

    public void setUltimasBusquedas(List<UltimaBusquedaDTO> ultimasBusquedas) {
        this.ultimasBusquedas = ultimasBusquedas;
    }

    public List<FavoritoDTO> getUltimosFavoritos() {
        return ultimosFavoritos;
    }

    public void setUltimosFavoritos(List<FavoritoDTO> ultimosFavoritos) {
        this.ultimosFavoritos = ultimosFavoritos;
    }

}
