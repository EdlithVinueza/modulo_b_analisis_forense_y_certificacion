package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen.*;


import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MetadatosImagenService {

    private final List<MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder>> extractores;

    public MetadatosImagenService() {
        this.extractores = Arrays.asList(
                new FileType(), // Identifica formato y extensión
                new Png(),      // Datos PNG (pHYs, gAMA, sRGB)
                new Jpeg(),     // Dimensiones JPG
                new Jfif(),     // DPI de JPG
                new Exif(),     // Respaldo de DPI y software
                new Xmp(),      // Metadatos extendidos (Software)
                new Icc()       // Perfil de color
        );
    }

    public MetadatosImagen procesarArchivo(File archivo) {
        MetadatosImagen.MetadatosImagenBuilder builder = MetadatosImagen.builder();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(archivo);

            for (MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> extractor : extractores) {
                try {
                    extractor.extraer(metadata, builder);
                } catch (Exception ex) {
                    System.err.println("Advertencia: El extractor " + extractor.getClass().getSimpleName() +
                            " falló, pero el análisis continuará. Error: " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error crítico procesando la imagen " + archivo.getName() + ": " + e.getMessage());
        }

        return builder.build();
    }
}
