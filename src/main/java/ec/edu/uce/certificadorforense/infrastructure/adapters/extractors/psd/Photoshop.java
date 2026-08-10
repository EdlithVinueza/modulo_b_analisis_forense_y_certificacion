package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.photoshop.PhotoshopDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class Photoshop implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {
    
    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        PhotoshopDirectory directory = metadata.getFirstDirectoryOfType(PhotoshopDirectory.class);
        if (directory != null) {
            builder.infoResolucion(directory.getDescription(PhotoshopDirectory.TAG_RESOLUTION_INFO))
                   .datosMiniatura(directory.getDescription(PhotoshopDirectory.TAG_THUMBNAIL_OLD));
        }
    }
}
