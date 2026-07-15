package uteq.edu.ec.sicrec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScopusInfoDTO {

    private boolean encontrado;

    private String pais;
    private Boolean accesoAbierto;
    private String enlaceScopus;
    private String publisher;

    private String citeScore;
    private String citeScoreYear;

    private String sjr;
    private String sjrYear;

    private String snip;
    private String snipYear;

    private String percentile;
    private String cuartil;

}