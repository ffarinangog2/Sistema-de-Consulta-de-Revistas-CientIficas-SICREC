package uteq.edu.ec.sicrec.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SerialTitleDTO {

    // Métricas
    private String citeScore;
    private String citeScoreYear;

    private String sjr;
    private String sjrYear;

    private String snip;
    private String snipYear;

    // Cuartil
    private String percentile;
    private String quartile;

    // Alias explícitos del mejor percentil/cuartil, sin eliminar los campos actuales.
    private String bestPercentile;
    private String bestQuartile;

    // Revista
    private String publisher;
    private String area;
    private String coverageStart;
    private String coverageEnd;

}
