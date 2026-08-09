package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.file.FileTypeDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class FileType implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        FileTypeDirectory directory = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
        if (directory == null) {
            return;
        }

        builder.nombreArchivoDetectado(directory.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME));
    }
}

