package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

public class ImageLoader {

    static {
        // Desactiva el uso de disco (caché temporal) para ImageIO,
        // usando memoria RAM directamente. Esto acelera drásticamente
        // el procesamiento de imágenes sin usar multihilo y sin alterar los píxeles (hashes).
        ImageIO.setUseCache(false);
    }

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
            
            validarTransparencia(img);
            return img;

        } catch (Exception e) {
            // Si la excepcion ya es de validacion forense, la propagamos directamente
            if (e.getMessage() != null && e.getMessage().contains("validación forense")) {
                throw new RuntimeException(e.getMessage());
            }
            
            System.err.println("Advertencia en carga optimizada de " + file.getName() + ": " + e.getMessage());
            // Fallback final a carga estándar
            try {
                BufferedImage imgFallback = ImageIO.read(file);
                if (imgFallback == null) {
                    throw new RuntimeException("ImageIO.read retornó nulo, formato no soportado o archivo corrupto.");
                }
                
                validarTransparencia(imgFallback);
                return imgFallback;
            } catch (Exception ex) {
                // Si la excepcion ya es de validacion forense, la propagamos
                if (ex.getMessage() != null && ex.getMessage().contains("validación forense")) {
                    throw new RuntimeException(ex.getMessage());
                }
                System.err.println("Fallo absoluto cargando imagen: " + ex.getMessage());
                throw new RuntimeException("Error crítico al leer los píxeles de " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Escanea los píxeles de la imagen. Si detecta transparencias (Alpha < 255), lanza una excepción
     * para bloquear el proceso por razones de integridad forense.
     */
    private static void validarTransparencia(BufferedImage img) {
        if (img != null && img.getColorModel().hasAlpha()) {
            int width = img.getWidth();
            int height = img.getHeight();
            // Escaneo a nivel de píxel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                    if (alpha < 255) {
                        throw new RuntimeException("Error de validación forense: La obra contiene transparencias. Para garantizar la integridad criptográfica entre el archivo fuente (PSD) y la imagen final, la obra debe tener una capa de fondo sólida (ej. blanca).");
                    }
                }
            }
        }
    }
}
