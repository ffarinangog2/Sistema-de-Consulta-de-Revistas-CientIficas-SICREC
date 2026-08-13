package uteq.edu.ec.crecuteq.dto;

import java.util.List;

public record RevistaPaginaDTO(
        List<RevistaDTO> contenido,
        int start,
        int count,
        long totalResults,
        boolean totalEstimado,
        boolean hasNext,
        int paginaActual,
        int totalPaginas
) {
}
