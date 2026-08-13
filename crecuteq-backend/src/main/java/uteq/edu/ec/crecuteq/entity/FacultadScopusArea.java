package uteq.edu.ec.crecuteq.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/** Equivalencia mantenible entre una facultad UTEQ y la clasificación ASJC. */
@Entity
@Table(name = "facultad_scopus_area")
@IdClass(FacultadScopusArea.Key.class)
public class FacultadScopusArea {
    @Id
    @Column(name = "facultad_codigo", nullable = false, length = 20)
    private String facultadCodigo;
    @Id
    @Column(name = "subarea_codigo", nullable = false, length = 4)
    private String subareaCodigo;

    public static class Key implements Serializable {
        private String facultadCodigo;
        private String subareaCodigo;

        public Key() {}

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Key other)) return false;
            return Objects.equals(facultadCodigo, other.facultadCodigo)
                    && Objects.equals(subareaCodigo, other.subareaCodigo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(facultadCodigo, subareaCodigo);
        }
    }
}
