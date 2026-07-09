package uteq.edu.ec.sicrec.projection;

import java.time.LocalDateTime;

public interface ReporteHistorialProjection {

    LocalDateTime getFecha();

    String getUsuario();

    String getTermino();

    Integer getResultados();

}