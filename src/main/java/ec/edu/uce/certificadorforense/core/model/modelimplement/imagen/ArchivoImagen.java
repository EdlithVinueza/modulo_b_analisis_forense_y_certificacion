package ec.edu.uce.certificadorforense.core.model.modelimplement.imagen;

import ec.edu.uce.certificadorforense.core.model.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;
import ec.edu.uce.certificadorforense.core.model.imagen.EstructuraImagen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ArchivoImagen extends ArchivoBase {
    private MetadatosImagen metadatos;
    private EstructuraImagen estructura;

    public MetadatosImagen getMetadatos() { return metadatos; }
}
