package ec.edu.uce.certificadorforense.core.ports;

import com.drew.metadata.Metadata;

public interface MetadataExtractor<T> {
    // T es el Builder del objeto que queremos llenar
    void extraer(Metadata metadata, T builder);
}