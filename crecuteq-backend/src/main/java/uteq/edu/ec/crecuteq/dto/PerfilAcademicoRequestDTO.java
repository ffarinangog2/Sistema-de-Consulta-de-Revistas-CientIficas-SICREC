package uteq.edu.ec.crecuteq.dto;

import jakarta.validation.constraints.Pattern;

// INICIO - Perfil Académico
public class PerfilAcademicoRequestDTO {

    @Pattern(
            regexp = "^$|^https://scholar\\.google\\.com/.+$",
            message = "Google Scholar debe ser una URL válida con el formato https://scholar.google.com/..."
    )
    private String googleScholar;

    @Pattern(
            regexp = "^$|^https://orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]/?$",
            message = "ORCID debe ser una URL válida con el formato https://orcid.org/0000-0000-0000-0000"
    )
    private String orcid;

    public PerfilAcademicoRequestDTO() {
    }

    public String getGoogleScholar() {
        return googleScholar;
    }

    public void setGoogleScholar(String googleScholar) {
        this.googleScholar = googleScholar;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}
// FIN - Perfil Académico
