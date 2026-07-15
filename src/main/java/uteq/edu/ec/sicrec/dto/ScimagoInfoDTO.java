package uteq.edu.ec.sicrec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScimagoInfoDTO {

    private boolean encontrado;

    private String titulo;
    private String issn;
    private String publisher;
    private String pais;
    private String region;

    private String sjr;
    private String cuartil;
    private String hIndex;
    private String cobertura;
    private String categorias;
    private String areas;
    private String rank;
    private String openAccess;

}