package ec.edu.uce.certificadorforense.core.model.modelimplement.psd;

import ec.edu.uce.certificadorforense.core.model.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ArchivoPSD extends ArchivoBase {
    private MetadatosPSD metadatos;
    private List<EstructuraCapaPSD> capas;

    public MetadatosPSD getMetadatos() { return metadatos; }
}
