package uteq.edu.ec.crecuteq.dto;

// Datos requeridos para restablecer la contraseña mediante un token.
public class RestablecerPasswordDTO {

    private String token;
    private String nuevaPassword;
    private String confirmarPassword;

    public RestablecerPasswordDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }

}
