package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen;

import com.drew.metadata.Metadata;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;

public class Png implements MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosImagen.MetadatosImagenBuilder builder) {
        com.drew.metadata.png.PngDirectory ihdr = metadata.getFirstDirectoryOfType(com.drew.metadata.png.PngDirectory.class);
        if (ihdr != null) {
            builder.ancho(ihdr.getInteger(com.drew.metadata.png.PngDirectory.TAG_IMAGE_WIDTH))
                    .alto(ihdr.getInteger(com.drew.metadata.png.PngDirectory.TAG_IMAGE_HEIGHT));
        }

        for (com.drew.metadata.Directory dir : metadata.getDirectories()) {
            String name = dir.getName();
            if ("PNG-pHYs".equals(name)) {
                builder.pixelesPorUnidadX(dir.getLongObject(16))
                        .pixelesPorUnidadY(dir.getLongObject(17)) // Recuperamos la Y
                        .unidadFisica(dir.getDescription(18));
            } else if ("PNG-sRGB".equals(name)) {
                builder.intentoInterpretacion(dir.getDescription(10));
            } else if ("PNG-gAMA".equals(name)) {
                builder.valorGamma(dir.getDescription(11));
            }
        }
    }
}
