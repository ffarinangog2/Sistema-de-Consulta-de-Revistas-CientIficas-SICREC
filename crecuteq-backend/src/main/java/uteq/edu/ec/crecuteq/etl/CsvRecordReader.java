package uteq.edu.ec.crecuteq.etl;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

// INICIO - Lector CSV común y reutilizable para ETL
/**
 * Lector RFC 4180 que admite comas, comillas y saltos de línea dentro de campos.
 */
public final class CsvRecordReader implements AutoCloseable {
    private final PushbackReader reader;
    private final char separador;

    public CsvRecordReader(Reader reader) {
        this(reader, ',');
    }

    public CsvRecordReader(Reader reader, char separador) {
        this.reader = new PushbackReader(reader, 1);
        this.separador = separador;
    }

    public List<String> leerRegistro() throws IOException {
        List<String> columnas = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean leyoContenido = false;

        int codigo;
        while ((codigo = reader.read()) != -1) {
            leyoContenido = true;
            char caracter = (char) codigo;
            if (caracter == '"') {
                if (entreComillas) {
                    int siguiente = reader.read();
                    if (siguiente == '"') {
                        campo.append('"');
                    } else {
                        entreComillas = false;
                        if (siguiente != -1) reader.unread(siguiente);
                    }
                } else if (campo.length() == 0) {
                    entreComillas = true;
                } else {
                    campo.append(caracter);
                }
            } else if (caracter == separador && !entreComillas) {
                columnas.add(campo.toString());
                campo.setLength(0);
            } else if ((caracter == '\n' || caracter == '\r') && !entreComillas) {
                if (caracter == '\r') {
                    int siguiente = reader.read();
                    if (siguiente != '\n' && siguiente != -1) reader.unread(siguiente);
                }
                columnas.add(campo.toString());
                return columnas;
            } else {
                campo.append(caracter);
            }
        }
        if (!leyoContenido && campo.length() == 0 && columnas.isEmpty()) return null;
        columnas.add(campo.toString());
        return columnas;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
// FIN - Lector CSV común y reutilizable para ETL
