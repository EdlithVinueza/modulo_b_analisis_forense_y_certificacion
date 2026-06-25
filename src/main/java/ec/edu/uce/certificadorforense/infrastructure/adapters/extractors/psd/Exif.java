package ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.psd;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import ec.edu.uce.certificadorforense.infrastructure.adapters.extractors.MetadataExtractor;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;

public class Exif implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {

    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0 != null) {
            if (builder == null) {
                return;
            }
            String software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);
            String autor = ifd0.getString(ExifIFD0Directory.TAG_ARTIST);

            if (software != null && !software.isBlank()) {
                builder.software(software);
            }
            if (autor != null && !autor.isBlank()) {
                builder.autor(autor);
            }
        }

        ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIfd != null) {
            String fecha = primerValorNoVacio(
                    subIfd.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL),
                    subIfd.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED)
            );

            if (fecha != null && !fecha.isBlank()) {
                builder.fechaCreacion(fecha);
            }
        }
    }

    private static String primerValorNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }
}
