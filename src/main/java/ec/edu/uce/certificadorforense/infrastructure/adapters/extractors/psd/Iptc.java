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

        String autor = primerValorNoVacio(
                directory.getString(IptcDirectory.TAG_BY_LINE),
                directory.getString(IptcDirectory.TAG_BY_LINE_TITLE)
        );
        if (autor != null && !autor.isBlank()) {
            builder.autor(autor);
        }

        String software = directory.getString(IptcDirectory.TAG_ORIGINATING_PROGRAM);
        if (software != null && !software.isBlank()) {
            builder.software(software);
        }

        String fecha = combinarFechaHora(
                primerValorNoVacio(directory.getString(IptcDirectory.TAG_DIGITAL_DATE_CREATED),
                        directory.getString(IptcDirectory.TAG_DATE_CREATED)),
                primerValorNoVacio(directory.getString(IptcDirectory.TAG_DIGITAL_TIME_CREATED),
                        directory.getString(IptcDirectory.TAG_TIME_CREATED))
        );
        if (fecha != null && !fecha.isBlank()) {
            builder.fechaCreacion(fecha);
        }
    }

    private static String combinarFechaHora(String fecha, String hora) {
        if (fecha == null || fecha.isBlank()) {
            return hora;
        }
        if (hora == null || hora.isBlank()) {
            return fecha;
        }
        return fecha + " " + hora;
    }

    private static String primerValorNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }
}
