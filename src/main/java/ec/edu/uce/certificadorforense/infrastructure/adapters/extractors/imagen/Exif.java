package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;

public class Exif implements MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> {
    @Override
    public void extraer(Metadata metadata, MetadatosImagen.MetadatosImagenBuilder builder) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (ifd0 != null) {
            Long res = ifd0.getLongObject(ExifIFD0Directory.TAG_X_RESOLUTION);
            if (res != null) {
                builder.pixelesPorUnidadX(res);
                builder.unidadFisica("Inches");
            }

            String software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);
            if (software != null && !software.isBlank()) {
                builder.software(software);
            }
        }
    }
}
