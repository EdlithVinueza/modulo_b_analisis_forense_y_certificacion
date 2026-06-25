package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.xmp.XmpDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

import java.util.Map;

public class Xmp implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    private static final String CREATOR_TOOL = "http://ns.adobe.com/xap/1.0/CreatorTool";
    private static final String CREATE_DATE = "http://ns.adobe.com/xap/1.0/CreateDate";
    private static final String MODIFY_DATE = "http://ns.adobe.com/xap/1.0/ModifyDate";
    private static final String CREATOR = "http://purl.org/dc/elements/1.1/creator";

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

        String autor = primerValorNoVacio(props.get(CREATOR), props.get("dc:creator"));
        if (autor != null && !autor.isBlank()) {
            builder.autor(normalizarListaXmp(autor));
        }

        String fecha = primerValorNoVacio(props.get(CREATE_DATE), props.get(MODIFY_DATE));
        if (fecha != null && !fecha.isBlank()) {
            builder.fechaCreacion(fecha);
        }
    }

    private static String primerValorNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }

    private static String normalizarListaXmp(String valor) {
        String limpio = valor.trim();
        if (limpio.startsWith("[") && limpio.endsWith("]") && limpio.length() > 1) {
            limpio = limpio.substring(1, limpio.length() - 1).trim();
        }
        return limpio;
    }
}
