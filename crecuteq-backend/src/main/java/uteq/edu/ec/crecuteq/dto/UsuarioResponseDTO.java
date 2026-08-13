package uteq.edu.ec.crecuteq.dto;

public class UsuarioResponseDTO {

    private Long id;
    private String nombreCompleto;
    private String correoInstitucional;
    private Boolean estado;
    private String rol;
    private String cargo;
    private String orcid;
    private String googleScholar;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(Long id,
                              String nombreCompleto,
                              String correoInstitucional,
                              Boolean estado,
                              String rol,
                              String cargo) {

        this(id, nombreCompleto, correoInstitucional, estado, rol, cargo, null, null);
    }

    public UsuarioResponseDTO(Long id,
                              String nombreCompleto,
                              String correoInstitucional,
                              Boolean estado,
                              String rol,
                              String cargo,
                              String orcid,
                              String googleScholar) {

        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correoInstitucional = correoInstitucional;
        this.estado = estado;
        this.rol = rol;
        this.cargo = cargo;
        this.orcid = orcid;
        this.googleScholar = googleScholar;
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public Boolean getEstado() {
        return estado;
    }

    public String getRol() {
        return rol;
    }

    public String getCargo() {
        return cargo;
    }

    public String getOrcid() {
        return orcid;
    }

    public String getGoogleScholar() {
        return googleScholar;
    }
}
