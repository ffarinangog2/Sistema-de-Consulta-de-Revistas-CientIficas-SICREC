package uteq.edu.ec.sicrec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.sicrec.entity.HistorialBusqueda;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.projection.ReporteHistorialProjection;
import uteq.edu.ec.sicrec.projection.TopBusquedaProjection;
import uteq.edu.ec.sicrec.projection.UltimaBusquedaProjection;

import java.util.List;

@Repository
public interface HistorialBusquedaRepository extends JpaRepository<HistorialBusqueda, Long> {
  long countByUsuarioId(Long usuarioId);
    List<HistorialBusqueda> findByUsuarioOrderByFechaBusquedaDesc(Usuario usuario);

    @Query(
            value = "SELECT * FROM fn_top_busquedas()",
            nativeQuery = true
    )
    List<TopBusquedaProjection> obtenerTopBusquedas();

    @Query(
            value = "SELECT * FROM fn_ultimas_busquedas()",
            nativeQuery = true
    )
    List<UltimaBusquedaProjection> obtenerUltimasBusquedas();


    @Query(
            value = "SELECT * FROM fn_reporte_historial()",
            nativeQuery = true
    )
    List<ReporteHistorialProjection> obtenerReporteHistorial();

    @Query(
            value = """
                SELECT *
                FROM fn_reporte_historial_fechas(
                    CAST(:fechaInicio AS DATE),
                    CAST(:fechaFin AS DATE)
                )
                """,
            nativeQuery = true
    )
    List<ReporteHistorialProjection> obtenerReporteHistorialPorFechas(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin
    );

    long countByUsuario_Id(Long usuarioId);

    List<HistorialBusqueda> findTop5ByUsuario_IdOrderByFechaBusquedaDesc(
            Long usuarioId
    );

}