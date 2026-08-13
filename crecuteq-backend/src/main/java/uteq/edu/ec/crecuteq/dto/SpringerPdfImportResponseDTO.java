package uteq.edu.ec.crecuteq.dto;

// INICIO - Importación PDF Springer
public class SpringerPdfImportResponseDTO {

    private int registrosLeidos;
    private int apcActualizados;
    private int registrosRechazados;
    private int registrosSinCoincidencia;
    private long tiempoEjecucionMs;

    public SpringerPdfImportResponseDTO() {
    }

    public SpringerPdfImportResponseDTO(
            int registrosLeidos,
            int apcActualizados,
            int registrosRechazados,
            int registrosSinCoincidencia,
            long tiempoEjecucionMs
    ) {
        this.registrosLeidos = registrosLeidos;
        this.apcActualizados = apcActualizados;
        this.registrosRechazados = registrosRechazados;
        this.registrosSinCoincidencia = registrosSinCoincidencia;
        this.tiempoEjecucionMs = tiempoEjecucionMs;
    }

    public int getRegistrosLeidos() { return registrosLeidos; }
    public int getApcActualizados() { return apcActualizados; }
    public int getRegistrosRechazados() { return registrosRechazados; }
    public int getRegistrosSinCoincidencia() { return registrosSinCoincidencia; }
    public long getTiempoEjecucionMs() { return tiempoEjecucionMs; }
}
// FIN - Importación PDF Springer
