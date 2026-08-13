package uteq.edu.ec.crecuteq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class DashboardDTO {

    private Long totalUsuarios;

    private Long totalFavoritos;

    private Long totalBusquedas;

    private Long totalRevistas;

    // Top 10 términos más buscados
    private List<TopBusquedaDTO> topBusquedas;

    private List<UltimaBusquedaDTO> ultimasBusquedas;

}