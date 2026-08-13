package uteq.edu.ec.crecuteq.etl;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

// INICIO - Adaptador común para importar catálogos descargados
public final class InMemoryMultipartFile implements MultipartFile {
    private final byte[] contenido;
    private final String nombreArchivo;
    private final String contentType;

    public InMemoryMultipartFile(
            byte[] contenido, String nombreArchivo, String contentType
    ) {
        this.contenido = contenido.clone();
        this.nombreArchivo = nombreArchivo;
        this.contentType = contentType;
    }

    @Override public String getName() { return "archivo"; }
    @Override public String getOriginalFilename() { return nombreArchivo; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return contenido.length == 0; }
    @Override public long getSize() { return contenido.length; }
    @Override public byte[] getBytes() { return contenido.clone(); }
    @Override public InputStream getInputStream() {
        return new ByteArrayInputStream(contenido);
    }
    @Override public void transferTo(File destino) throws IOException {
        Files.write(destino.toPath(), contenido);
    }
}
// FIN - Adaptador común para importar catálogos descargados
