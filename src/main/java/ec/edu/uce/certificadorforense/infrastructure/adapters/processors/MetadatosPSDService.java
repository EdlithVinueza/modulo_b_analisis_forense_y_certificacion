package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MetadatosPSDService {
    private final List<MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder>> extractors;

    public MetadatosPSDService() {
        // Ordenados de MENOS fiable a MÁS fiable (Last-Win)
        this.extractors = Arrays.asList(
                new Jfif(),      // Muy genérico (prioridad baja)
                new Exif(),      // Datos de cámara/motor (prioridad media)
                new Iptc(),      // Datos de prensa (prioridad media)
                new Xmp(),       // Datos Adobe (prioridad alta)
                new Photoshop(), // Recursos específicos (prioridad alta)
                new PsdHeader(), // LA VERDAD BINARIA (prioridad máxima)
                new FileType()   // Identificación final
        );
    }

    public MetadatosPSD procesarArchivo(File archivo) {
        MetadatosPSD.MetadatosPSDBuilder builder = MetadatosPSD.builder();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(archivo);
            for (MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> extractor : extractors) {
                try {
                    extractor.extraer(metadata, builder);
                } catch (Exception ex) {
                    System.err.println("Advertencia en PSD: El extractor " + extractor.getClass().getSimpleName() +
                            " falló, pero el análisis continuará. Error: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
        }
        return builder.build();
    }
}
