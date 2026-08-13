package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uteq.edu.ec.crecuteq.entity.FacultadScopusArea;

import java.util.List;

public interface FacultadScopusAreaRepository
        extends JpaRepository<FacultadScopusArea, FacultadScopusArea.Key> {
    @Query("""
            select distinct mapping.subareaCodigo
            from FacultadScopusArea mapping
            where upper(mapping.facultadCodigo) = upper(:facultad)
            order by mapping.subareaCodigo
            """)
    List<String> findSubareaCodesByFacultad(@Param("facultad") String facultad);

    @Query(value = """
            SELECT facultad_codigo, facultad_nombre
            FROM facultad
            ORDER BY facultad_nombre
            """, nativeQuery = true)
    List<Object[]> findAllFacultades();
}
