package uteq.edu.ec.crecuteq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    // INICIO - Subject Area
    private List<SubjectAreaDTO> subjectAreas;
    // FIN - Subject Area

    // INICIO - Tipo de fuente
    private String tipoFuente;
    // FIN - Tipo de fuente

    // INICIO - Open Access
    private String tipoOpenAccess;
    // FIN - Open Access

    // INICIO - Cobertura
    private String coverageStartYear;
    private String coverageEndYear;
    // FIN - Cobertura

    private String citeScore;
    private String citeScoreYear;

    private String sjr;
    private String sjrYear;

    private String snip;
    private String snipYear;

    private String percentile;
    private String cuartil;

    // Se conservan percentile y cuartil para compatibilidad con el frontend actual.
    private String bestPercentile;
    private String bestQuartile;

}
