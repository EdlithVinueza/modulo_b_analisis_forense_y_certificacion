package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.xmp.XmpDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

import java.util.Map;

public class Xmp implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    private static final String CREATOR_TOOL = "http://ns.adobe.com/xap/1.0/CreatorTool";

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        XmpDirectory directory = metadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (directory == null) {
            return;
        }

        Map<String, String> props = directory.getXmpProperties();
        if (props == null || props.isEmpty()) {
            return;
        }

        String software = primerValorNoVacio(props.get(CREATOR_TOOL), props.get("xmp:CreatorTool"));
        if (software != null && !software.isBlank()) {
            builder.software(software);
        }
    }

    private static String primerValorNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }
}

