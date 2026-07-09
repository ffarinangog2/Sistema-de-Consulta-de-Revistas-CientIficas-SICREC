package uteq.edu.ec.sicrec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UltimaBusquedaDTO {

    private LocalDateTime fecha;

    private String usuario;

    private String termino;

    private Integer resultados;

}