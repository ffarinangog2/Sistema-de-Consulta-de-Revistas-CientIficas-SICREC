package uteq.edu.ec.sicrec.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "favoritos")
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "source_id")
    private String sourceId;

    private String issn;

    @Column(columnDefinition = "TEXT")
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String revista;

    private String cuartil;

    private Integer anio;

    @Column(name = "fecha_guardado")
    private LocalDateTime fechaGuardado;

}