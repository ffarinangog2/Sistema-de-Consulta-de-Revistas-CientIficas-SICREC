package uteq.edu.ec.crecuteq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

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
    private String pais;
    private String cuartil;
    private String origenCuartil;
    private boolean encontradaEnScimago;

    // ===============================
    // Bloques totalmente independientes por fuente
    // ===============================
    private ScopusInfoDTO scopus;   // null si no se encontró en Scopus
    // Compatibilidad del contrato existente: SCImago solo aporta país y cuartil.
    private ScimagoInfoDTO scimago;
    // INICIO - Integración Springer en búsquedas
    private SpringerInfoDTO springer;

    // INICIO - Integración común de proveedores (DOAJ y estrategias futuras)
    private JournalInfo doaj;
    private Map<String, JournalInfo> proveedores;
    // FIN - Integración común de proveedores (DOAJ y estrategias futuras)
    // FIN - Integración Springer en búsquedas

}
