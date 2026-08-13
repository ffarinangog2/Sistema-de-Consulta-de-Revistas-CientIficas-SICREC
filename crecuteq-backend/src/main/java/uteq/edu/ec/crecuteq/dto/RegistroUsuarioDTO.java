package uteq.edu.ec.crecuteq.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegistroUsuarioDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre completo debe tener entre 3 y 150 caracteres")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "El nombre completo contiene caracteres no permitidos")
    private String nombreCompleto;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El correo institucional no tiene un formato válido")
    @Size(max = 180, message = "El correo institucional no puede superar 180 caracteres")
    @Pattern(regexp = "(?i)^[A-Z0-9._%+-]+@uteq\\.edu\\.ec$",
            message = "Debe utilizar un correo institucional @uteq.edu.ec")
    private String correoInstitucional;

    private String tipoUsuario;
    private Long rolId;
    @NotNull(message = "Debe seleccionar un cargo")
    private Long cargoId;

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

    public RegistroUsuarioDTO() {
    }

    public RegistroUsuarioDTO(
            String nombreCompleto,
            String correoInstitucional,
            String tipoUsuario
    ) {
        this.nombreCompleto = nombreCompleto;
        this.correoInstitucional = correoInstitucional;
        this.tipoUsuario = tipoUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public Long getRolId() { return rolId; }
    public void setRolId(Long rolId) { this.rolId = rolId; }
    public Long getCargoId() { return cargoId; }
    public void setCargoId(Long cargoId) { this.cargoId = cargoId; }

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
