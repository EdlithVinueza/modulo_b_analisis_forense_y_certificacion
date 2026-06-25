package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.jfif.JfifDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class Jfif implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        JfifDirectory directory = metadata.getFirstDirectoryOfType(JfifDirectory.class);
        if (directory == null) {
            return;
        }

        String resolucion = construirResolucion(directory);
        if (resolucion != null && !resolucion.isBlank()) {
            builder.infoResolucion(resolucion);
        }

        String miniatura = construirMiniatura(directory);
        if (miniatura != null && !miniatura.isBlank()) {
            builder.datosMiniatura(miniatura);
        }
    }

    private static String construirResolucion(JfifDirectory directory) {
        String x = directory.getDescription(JfifDirectory.TAG_RESX);
        String y = directory.getDescription(JfifDirectory.TAG_RESY);
        String units = directory.getDescription(JfifDirectory.TAG_UNITS);

        if ((x == null || x.isBlank()) && (y == null || y.isBlank())) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(x != null ? x : "?");
        sb.append("x");
        sb.append(y != null ? y : "?");
        if (units != null && !units.isBlank()) {
            sb.append(' ').append(units);
        }
        return sb.toString();
    }

    private static String construirMiniatura(JfifDirectory directory) {
        if (!directory.containsTag(JfifDirectory.TAG_THUMB_WIDTH) || !directory.containsTag(JfifDirectory.TAG_THUMB_HEIGHT)) {
            return null;
        }
        return "JFIF thumbnail "
                + directory.getDescription(JfifDirectory.TAG_THUMB_WIDTH)
                + "x"
                + directory.getDescription(JfifDirectory.TAG_THUMB_HEIGHT);
    }
}
