package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.imagen;

import com.drew.metadata.Metadata;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;

public class FileType implements MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder> {
    @Override
    public void extraer(Metadata metadata, MetadatosImagen.MetadatosImagenBuilder builder) {
        com.drew.metadata.file.FileTypeDirectory dir = metadata.getFirstDirectoryOfType(com.drew.metadata.file.FileTypeDirectory.class);
        if (dir != null) {
            builder.tipoMime(dir.getString(com.drew.metadata.file.FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE))
                    .extensionReal(dir.getString(com.drew.metadata.file.FileTypeDirectory.TAG_EXPECTED_FILE_NAME_EXTENSION));
        }
    }
}
