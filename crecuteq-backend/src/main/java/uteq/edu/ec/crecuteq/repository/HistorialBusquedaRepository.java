package uteq.edu.ec.crecuteq.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.crecuteq.entity.HistorialBusqueda;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.projection.ReporteHistorialProjection;
import uteq.edu.ec.crecuteq.projection.ReporteResumenProjection;
import uteq.edu.ec.crecuteq.projection.ReporteSerieProjection;
import uteq.edu.ec.crecuteq.projection.TopBusquedaProjection;
import uteq.edu.ec.crecuteq.projection.UltimaBusquedaProjection;

import java.util.List;

@Repository
public interface HistorialBusquedaRepository extends JpaRepository<HistorialBusqueda, Long> {
    long countByCantidadResultadosGreaterThan(Integer cantidadResultados);

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

    String FILTRO_REPORTE = """
            FROM historial_busqueda h
            JOIN usuario u ON u.id_usuario = h.usuario_id
            WHERE (CAST(:fechaInicio AS TEXT) IS NULL OR h.fecha_busqueda >= CAST(:fechaInicio AS DATE))
              AND (CAST(:fechaFin AS TEXT) IS NULL OR h.fecha_busqueda < CAST(:fechaFin AS DATE) + INTERVAL '1 day')
            """;

    @Query(value = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE COALESCE(h.cantidad_resultados, 0) > 0) AS "conResultados",
                   COUNT(*) FILTER (WHERE COALESCE(h.cantidad_resultados, 0) = 0) AS "sinResultados",
                   COUNT(DISTINCT h.usuario_id) AS usuarios
            """ + FILTRO_REPORTE, nativeQuery = true)
    ReporteResumenProjection obtenerResumenReporte(
            @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin);

    @Query(value = """
            SELECT TO_CHAR(CAST(h.fecha_busqueda AS DATE), 'YYYY-MM-DD') AS etiqueta, COUNT(*) AS total
            """ + FILTRO_REPORTE + """
            GROUP BY CAST(h.fecha_busqueda AS DATE)
            ORDER BY CAST(h.fecha_busqueda AS DATE)
            """, nativeQuery = true)
    List<ReporteSerieProjection> obtenerBusquedasPorDia(
            @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin);

    @Query(value = """
            SELECT MIN(h.termino_busqueda) AS etiqueta, COUNT(*) AS total
            """ + FILTRO_REPORTE + """
            GROUP BY LOWER(TRIM(h.termino_busqueda))
            ORDER BY total DESC, etiqueta
            LIMIT 10
            """, nativeQuery = true)
    List<ReporteSerieProjection> obtenerTerminosReporte(
            @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin);

    @Query(value = """
            SELECT u.nombre_completo AS etiqueta, COUNT(*) AS total
            """ + FILTRO_REPORTE + """
            GROUP BY u.id_usuario, u.nombre_completo
            ORDER BY total DESC, etiqueta
            LIMIT 10
            """, nativeQuery = true)
    List<ReporteSerieProjection> obtenerUsuariosReporte(
            @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin);

    @Query(value = """
            SELECT h.fecha_busqueda AS fecha, u.nombre_completo AS usuario,
                   h.termino_busqueda AS termino, h.cantidad_resultados AS resultados
            """ + FILTRO_REPORTE + """
            ORDER BY h.fecha_busqueda DESC
            LIMIT :limite OFFSET :desplazamiento
            """, nativeQuery = true)
    List<ReporteHistorialProjection> obtenerPaginaReporte(
            @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin,
            @Param("limite") int limite, @Param("desplazamiento") int desplazamiento);

    long countByUsuario_Id(Long usuarioId);

    List<HistorialBusqueda> findTop5ByUsuario_IdOrderByFechaBusquedaDesc(
            Long usuarioId
    );

}
