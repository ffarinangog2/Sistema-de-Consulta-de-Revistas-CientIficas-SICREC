package uteq.edu.ec.sicrec.dto;

import java.time.LocalDateTime;

// INICIO - Endpoints de auditoría
public class AuditoriaResponseDTO {

    private final Long id;
    private final UsuarioAuditoriaDTO usuario;
    private final String modulo;
    private final String accion;
    private final String descripcion;
    private final LocalDateTime fechaAccion;
    private final String ipOrigen;
    private final String resultado;

    public AuditoriaResponseDTO(
            Long id,
            UsuarioAuditoriaDTO usuario,
            String modulo,
            String accion,
            String descripcion,
            LocalDateTime fechaAccion,
            String ipOrigen,
            String resultado
    ) {

        this.id = id;
        this.usuario = usuario;
        this.modulo = modulo;
        this.accion = accion;
        this.descripcion = descripcion;
        this.fechaAccion = fechaAccion;
        this.ipOrigen = ipOrigen;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public UsuarioAuditoriaDTO getUsuario() {
        return usuario;
    }

    public String getModulo() {
        return modulo;
    }

    public String getAccion() {
        return accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaAccion() {
        return fechaAccion;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public String getResultado() {
        return resultado;
    }

    public String getNombreUsuario() {

        return usuario != null
                ? usuario.getUsuario()
                : "Sistema";
    }
}
// FIN - Endpoints de auditoría
