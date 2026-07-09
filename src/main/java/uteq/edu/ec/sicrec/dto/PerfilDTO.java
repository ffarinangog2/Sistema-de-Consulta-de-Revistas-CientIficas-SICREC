package uteq.edu.ec.sicrec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {

    private String nombreCompleto;

    private String correoInstitucional;

    private String rol;

    private String cargo;

    private Boolean estado;

}