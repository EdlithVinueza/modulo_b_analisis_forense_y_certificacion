package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen;

import com.drew.metadata.Metadata;
import com.drew.metadata.xmp.XmpDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;
import java.util.Map;

public class Xmp implements MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> {
    @Override
    public void extraer(Metadata metadata, MetadatosImagen.MetadatosImagenBuilder builder) {
        XmpDirectory directory = metadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (directory != null && directory.getXmpProperties() != null) {
            String software = directory.getXmpProperties().get("xmp:CreatorTool");
            if (software == null || software.isBlank()) {
                software = directory.getXmpProperties().get("http://ns.adobe.com/xap/1.0/CreatorTool");
            }
            if (software != null && !software.isBlank()) {
                builder.software(software);
            }
        }
    }
}
