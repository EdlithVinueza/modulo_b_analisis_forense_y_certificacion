package ec.edu.uce.certificadorforense.infrastructure.adapters.rest.util;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utilitario para el manejo seguro y limpio de archivos temporales multipart en endpoints REST.
 */
public final class TempFileUtil {

    private TempFileUtil() {}

    public static File crearTemporal(FileUpload upload, String prefijo) throws IOException {
        if (upload == null) return null;
        if (upload.uploadedFile() != null && Files.exists(upload.uploadedFile())) {
            Path tempPath = Files.createTempFile(prefijo + "-", "-" + upload.fileName());
            try {
                // Movimiento atómico instantáneo (rename en el mismo filesystem sin I/O en disco)
                Files.move(upload.uploadedFile(), tempPath, StandardCopyOption.REPLACE_EXISTING);
                return tempPath.toFile();
            } catch (Exception e) {
                // Si el filesystem no permite move atómico, retornamos el archivo temporal directo
                return upload.uploadedFile().toFile();
            }
        }
        return null;
    }

    public static void borrarSilencioso(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (Exception ignored) {}
        }
    }
}
