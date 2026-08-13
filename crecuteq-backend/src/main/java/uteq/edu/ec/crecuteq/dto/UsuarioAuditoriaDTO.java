package uteq.edu.ec.crecuteq.dto;

// INICIO - Endpoints de auditoría
public class UsuarioAuditoriaDTO {

    private final Long id;
    private final String usuario;
    private final String nombreCompleto;

    public UsuarioAuditoriaDTO(
            Long id,
            String usuario,
            String nombreCompleto
    ) {

        this.id = id;
        this.usuario = usuario;
        this.nombreCompleto = nombreCompleto;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }
}
// FIN - Endpoints de auditoría
