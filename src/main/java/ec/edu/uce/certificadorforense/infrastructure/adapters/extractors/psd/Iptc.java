package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.iptc.IptcDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class Iptc implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        IptcDirectory directory = metadata.getFirstDirectoryOfType(IptcDirectory.class);
        if (directory == null) {
            return;
        }

        String software = directory.getString(IptcDirectory.TAG_ORIGINATING_PROGRAM);
        if (software != null && !software.isBlank()) {
            builder.software(software);
        }
    }
}

