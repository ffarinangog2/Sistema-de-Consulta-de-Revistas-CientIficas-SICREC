package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// INICIO - Perfil Académico
@Entity
@Table(
        name = "perfil_academico",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_perfil_academico_usuario",
                columnNames = "usuario_id"
        )
)
public class PerfilAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil_academico")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "google_scholar", length = 500)
    private String googleScholar;

    @Column(name = "orcid", length = 100)
    private String orcid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PerfilAcademico() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
// FIN - Perfil Académico
