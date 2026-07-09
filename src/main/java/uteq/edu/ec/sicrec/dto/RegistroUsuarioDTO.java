package uteq.edu.ec.sicrec.dto;

public class RegistroUsuarioDTO {

    private String nombreCompleto;

    private String correoInstitucional;

    private String tipoUsuario;

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

}