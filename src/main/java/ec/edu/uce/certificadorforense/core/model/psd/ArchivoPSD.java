package ec.edu.uce.certificadorforense.core.model.psd;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ec.edu.uce.certificadorforense.core.modelimplement.ArchivoBase;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ArchivoPSD extends ArchivoBase {
    private MetadatosPSD metadatos;
    private List<EstructuraCapaPSD> capas; // Capas físicas extraídas

    @Override
    public MetadatosPSD getMetadatos() { return metadatos; }
}

