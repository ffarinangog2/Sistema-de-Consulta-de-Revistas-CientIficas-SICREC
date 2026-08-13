package uteq.edu.ec.crecuteq.dto;

// INICIO - Importación Springer
public class CatalogImportResponseDTO {

    private int registrosLeidos;
    private int registrosInsertados;
    private int registrosActualizados;
    private int registrosRechazados;
    private long tiempoEjecucionMs;

    public CatalogImportResponseDTO() {
    }

    public CatalogImportResponseDTO(
            int registrosLeidos,
            int registrosInsertados,
            int registrosActualizados,
            int registrosRechazados,
            long tiempoEjecucionMs
    ) {
        this.registrosLeidos = registrosLeidos;
        this.registrosInsertados = registrosInsertados;
        this.registrosActualizados = registrosActualizados;
        this.registrosRechazados = registrosRechazados;
        this.tiempoEjecucionMs = tiempoEjecucionMs;
    }

    public int getRegistrosLeidos() {
        return registrosLeidos;
    }

    public int getRegistrosInsertados() {
        return registrosInsertados;
    }

    public int getRegistrosActualizados() {
        return registrosActualizados;
    }

    public int getRegistrosRechazados() {
        return registrosRechazados;
    }

    public long getTiempoEjecucionMs() {
        return tiempoEjecucionMs;
    }
}
// FIN - Importación Springer
