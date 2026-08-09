package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class Exif implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0 != null) {
            if (builder == null) {
                return;
            }
            String software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);

            if (software != null && !software.isBlank()) {
                builder.software(software);
            }
        }
    }
}

