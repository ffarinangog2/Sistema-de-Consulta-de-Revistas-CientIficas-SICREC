package uteq.edu.ec.crecuteq.dto;

import java.util.List;

public record ReporteAnaliticaDTO(
        Resumen resumen,
        List<Serie> busquedasPorDia,
        List<Serie> terminosMasConsultados,
        List<Serie> usuariosMasActivos,
        Pagina pagina
) {
    public record Resumen(long total, long conResultados, long sinResultados, long usuarios) { }
    public record Serie(String etiqueta, long total) { }
    public record Pagina(List<ReporteHistorialDTO> contenido, int paginaActual,
                         int tamano, long totalRegistros, int totalPaginas) { }
}
