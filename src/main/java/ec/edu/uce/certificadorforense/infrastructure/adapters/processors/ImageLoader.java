package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

public class ImageLoader {

    /**
     * Carga una imagen o composite de PSD usando submuestreo agresivo (subsampling)
     * si las dimensiones son grandes, evitando allocations masivos en memoria RAM.
     */
    public static BufferedImage loadWithSubsampling(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                // Fallback directo a ImageIO normal si no hay lector especializado
                return ImageIO.read(file);
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            ImageReadParam param = reader.getDefaultReadParam();
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            // Si las dimensiones superan los 2000 píxeles, aplicamos submuestreo dinámico moderado.
            // Un subsampling muy agresivo (saltarse demasiados píxeles) causa "Aliasing"
            // y destruye por completo el pHash si las imágenes varían mínimamente en tamaño.
            if (width > 2000 || height > 2000) {
                int mayorDimension = Math.max(width, height);
                // Submuestreamos para que la imagen resultante tenga unos ~1000px de lado maximo
                // Consumirá maximo ~4MB de RAM, lo cual es muy seguro y no daña el pHash.
                int subsampling = Math.max(1, mayorDimension / 1000);
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
            }

            BufferedImage img = reader.read(0, param);
            reader.dispose();
            if (img == null) throw new RuntimeException("Lector de imagen retornó nulo.");
            return img;

        } catch (Exception e) {
            System.err.println("Advertencia en carga optimizada de " + file.getName() + ": " + e.getMessage());
            // Fallback final a carga estándar
            try {
                BufferedImage imgFallback = ImageIO.read(file);
                if (imgFallback == null) {
                    throw new RuntimeException("ImageIO.read retornó nulo, formato no soportado o archivo corrupto.");
                }
                return imgFallback;
            } catch (Exception ex) {
                System.err.println("Fallo absoluto cargando imagen: " + ex.getMessage());
                throw new RuntimeException("Error crítico al leer los píxeles de " + file.getName() + ": " + ex.getMessage());
            }
        }
    }
}
