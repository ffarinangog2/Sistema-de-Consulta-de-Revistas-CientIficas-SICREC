package uteq.edu.ec.crecuteq.dto;

import java.time.LocalDateTime;

// INICIO - Perfil Académico
public class PerfilAcademicoResponseDTO {

    private Long id;
    private Long usuarioId;
    private String googleScholar;
    private String orcid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean tieneGoogleScholar;
    private boolean tieneOrcid;

    public PerfilAcademicoResponseDTO() {
    }

    public PerfilAcademicoResponseDTO(
            Long id,
            Long usuarioId,
            String googleScholar,
            String orcid,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean tieneGoogleScholar,
            boolean tieneOrcid
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.googleScholar = googleScholar;
        this.orcid = orcid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tieneGoogleScholar = tieneGoogleScholar;
        this.tieneOrcid = tieneOrcid;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getGoogleScholar() {
        return googleScholar;
    }

    public String getOrcid() {
        return orcid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTieneGoogleScholar() {
        return tieneGoogleScholar;
    }

    public boolean isTieneOrcid() {
        return tieneOrcid;
    }
}
// FIN - Perfil Académico
