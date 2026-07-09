package uteq.edu.ec.sicrec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevistaDTO {

    // Datos obtenidos de Scopus
    private String titulo;
    private String revista;
    private String issn;
    private String eIssn;
    private String sourceId;
    private String fecha;
    private String pais;
    private Boolean accesoAbierto;
    private String enlaceScopus;

    // Datos obtenidos de SCImago
//    private String cuartil;
//    private String sjr;
//    private String hIndex;
//    private String cobertura;
//    private String categorias;
//    private String areas;
    // Métricas de la revista
    // Métricas de la revista (Scopus)

    private String citeScore;
    private String citeScoreYear;

    private String sjr;
    private String sjrYear;

    private String snip;
    private String snipYear;

    private String percentile;
    private String cuartil;

    private String publisher;

    private String hIndex;
    private String cobertura;
    private String categorias;
    private String areas;


}