package uteq.edu.ec.crecuteq.dto;

public class LoginResponseDTO {

    private Long id;
    private String nombreCompleto;
    private String correoInstitucional;
    private String rol;
    private String mensaje;
    private String token;
    private Boolean debeCambiarPassword;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(
            Long id,
            String nombreCompleto,
            String correoInstitucional,
            String rol,
            String mensaje,
            String token,
            Boolean debeCambiarPassword) {

        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correoInstitucional = correoInstitucional;
        this.rol = rol;
        this.mensaje = mensaje;
        this.token = token;
        this.debeCambiarPassword = debeCambiarPassword;
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

    public String getRol() {
        return rol;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getToken() {
        return token;
    }

    public Boolean getDebeCambiarPassword() {
        return debeCambiarPassword;
    }

}