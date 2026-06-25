package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.icc.IccDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class IccProfile implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        IccDirectory directory = metadata.getFirstDirectoryOfType(IccDirectory.class);
        if (directory != null) {
            builder.tienePerfilIcc(true)
                   .descripcionPerfilIcc(directory.getDescription(IccDirectory.TAG_TAG_desc))
                   .clasePerfilIcc(directory.getDescription(IccDirectory.TAG_PROFILE_CLASS))
                   .espacioColorIcc(directory.getDescription(IccDirectory.TAG_COLOR_SPACE))
                   .copyrightIcc(directory.getDescription(IccDirectory.TAG_TAG_cprt));
        }
    }
}
