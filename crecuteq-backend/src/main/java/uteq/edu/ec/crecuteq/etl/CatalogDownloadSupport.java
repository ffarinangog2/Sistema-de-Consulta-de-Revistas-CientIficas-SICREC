package uteq.edu.ec.crecuteq.etl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// INICIO - Soporte común de detección de cambios de catálogos
public final class CatalogDownloadSupport {
    private CatalogDownloadSupport() {
    }

    public static String calcularSha256(byte[] archivo) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(archivo)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    public static String leerUltimoHash(Path hashPath) throws IOException {
        return Files.exists(hashPath)
                ? Files.readString(hashPath).trim()
                : "";
    }

    public static void guardarHash(
            Path hashPath, String hash, String prefijoTemporal
    ) throws IOException {
        Path absoluto = hashPath.toAbsolutePath();
        Path directorio = absoluto.getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }

        Path temporal = Files.createTempFile(
                directorio, prefijoTemporal, ".sha256.tmp"
        );
        try {
            Files.writeString(temporal, hash);
            try {
                Files.move(temporal, absoluto,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                Files.move(temporal, absoluto,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
// FIN - Soporte común de detección de cambios de catálogos
