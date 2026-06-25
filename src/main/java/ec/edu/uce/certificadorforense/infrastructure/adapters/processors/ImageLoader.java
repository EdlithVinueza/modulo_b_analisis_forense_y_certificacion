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

            // Si las dimensiones superan 512 píxeles, aplicamos submuestreo dinámico.
            // Para pHash, un submuestreo que nos deje una imagen de ~64x64 es ideal,
            // ahorrando el 99.9% de memoria en imágenes ultra pesadas.
            if (width > 512 || height > 512) {
                int menorDimension = Math.min(width, height);
                int subsampling = Math.max(1, menorDimension / 64);
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
            }

            BufferedImage img = reader.read(0, param);
            reader.dispose();
            return img;

        } catch (Exception e) {
            System.err.println("Advertencia en carga optimizada de " + file.getName() + ": " + e.getMessage());
            // Fallback final a carga estándar
            try {
                return ImageIO.read(file);
            } catch (Exception ex) {
                System.err.println("Fallo absoluto cargando imagen: " + ex.getMessage());
                return null;
            }
        }
    }
}
