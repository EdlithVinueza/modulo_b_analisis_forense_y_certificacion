package ec.edu.uce.certificadorforense.core.model.imagen;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ArchivoImagen extends ArchivoBase {
    private MetadatosImagen metadatos;
    private EstructuraImagen estructura; // Lo binario manual

    @Override
    public MetadatosImagen getMetadatos() { return metadatos; }
}
