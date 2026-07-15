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

    // ===============================
    // Datos genéricos / identificación
    // (usados por ejemplo para guardar en favoritos,
    // sin importar de qué fuente vengan)
    // ===============================
    private String titulo;
    private String revista;
    private String issn;
    private String eIssn;
    private String sourceId;
    private String fecha;
    private String cuartil; // cuartil "principal": Scopus si existe, si no SCImago

    // ===============================
    // Bloques totalmente independientes por fuente
    // ===============================
    private ScopusInfoDTO scopus;   // null si no se encontró en Scopus
    private ScimagoInfoDTO scimago; // null si no se encontró en SCImago

}