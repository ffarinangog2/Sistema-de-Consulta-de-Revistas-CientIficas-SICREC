package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auditoria",
        indexes = {
                @Index(name = "idx_auditoria_fecha", columnList = "fecha_accion"),
                @Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
                @Index(name = "idx_auditoria_modulo", columnList = "modulo"),
                @Index(name = "idx_auditoria_accion", columnList = "accion")
        }
)
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // INICIO - Auditoría general
    @Column(length = 50)
    private String modulo;

    @Column(length = 100)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_accion")
    private LocalDateTime fechaAccion;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(length = 10)
    private String resultado;
    // FIN - Auditoría general

    public Auditoria() {
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

    // INICIO - Auditoría general
    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaAccion() {
        return fechaAccion;
    }

    public void setFechaAccion(LocalDateTime fechaAccion) {
        this.fechaAccion = fechaAccion;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    @PrePersist
    public void prePersist() {

        if (fechaAccion == null) {
            fechaAccion = LocalDateTime.now();
        }
    }
    // FIN - Auditoría general
}
