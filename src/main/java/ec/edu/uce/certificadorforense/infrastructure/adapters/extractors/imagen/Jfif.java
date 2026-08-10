package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen;

import com.drew.metadata.Metadata;
import com.drew.metadata.jfif.JfifDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;

public class Jfif implements MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> {
    @Override
    public void extraer(Metadata metadata, MetadatosImagen.MetadatosImagenBuilder builder) {
        JfifDirectory dir = metadata.getFirstDirectoryOfType(JfifDirectory.class);
        if (dir != null) {
            Integer x = dir.getInteger(JfifDirectory.TAG_RESX);
            Integer y = dir.getInteger(JfifDirectory.TAG_RESY);
            if (x != null) builder.pixelesPorUnidadX(x);
            if (y != null) builder.pixelesPorUnidadY(y);
            builder.unidadFisica(dir.getDescription(JfifDirectory.TAG_UNITS));
        }
    }
}
