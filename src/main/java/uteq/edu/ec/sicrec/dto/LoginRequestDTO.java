package uteq.edu.ec.sicrec.dto;

public class LoginRequestDTO {

    private String usuario;
    private String password;

    public LoginRequestDTO() {
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}