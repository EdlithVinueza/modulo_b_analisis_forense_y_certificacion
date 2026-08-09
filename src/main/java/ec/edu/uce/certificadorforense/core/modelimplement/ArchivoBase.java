package ec.edu.uce.certificadorforense.core.modelimplement;

import ec.edu.uce.certificadorforense.core.modelinterface.Analizable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class ArchivoBase implements Analizable {
    protected String nombreArchivo;
    protected byte[] contenidoBytes;
    protected long tamanoBytes;

    @Override
    public String getNombreArchivo() { return nombreArchivo; }
    @Override
    public long getTamanoBytes() { return tamanoBytes; }
}
