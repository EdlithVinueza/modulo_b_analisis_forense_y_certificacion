package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class PsdHeader implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {
  
    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        PsdHeaderDirectory directory = metadata.getFirstDirectoryOfType(PsdHeaderDirectory.class);
        if (directory != null) {
            builder.cantidadCanales(directory.getInteger(PsdHeaderDirectory.TAG_CHANNEL_COUNT))
                   .altoImagen(directory.getInteger(PsdHeaderDirectory.TAG_IMAGE_HEIGHT))
                   .anchoImagen(directory.getInteger(PsdHeaderDirectory.TAG_IMAGE_WIDTH))
                   .bitsPorCanal(directory.getInteger(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL))
                   .modoColor(directory.getDescription(PsdHeaderDirectory.TAG_COLOR_MODE));
        }
    }
}
