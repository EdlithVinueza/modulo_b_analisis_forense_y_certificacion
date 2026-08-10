package ec.edu.uce.certificadorforense.core.model.modelimplement;

import ec.edu.uce.certificadorforense.core.model.modelinterface.Analizable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.File;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ArchivoBase implements Analizable {
    protected File archivo;
    protected String nombreArchivo;
    protected long tamanoBytes;
    protected byte[] contenidoBytes;

    @Override
    public File getArchivo() {
        return archivo;
    }

    @Override
    public String getNombreArchivo() {
        return nombreArchivo;
    }

    @Override
    public long getTamanoBytes() {
        return tamanoBytes;
    }
}
