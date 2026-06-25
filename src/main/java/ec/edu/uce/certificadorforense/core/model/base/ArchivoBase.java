package ec.edu.uce.certificadorforense.core.model.base;

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
    protected String rutaAbsoluta;
    protected long tamanoBytes;

    @Override
    public String getNombreArchivo() { return nombreArchivo; }
    @Override
    public String getRutaAbsoluta() { return rutaAbsoluta; }
    @Override
    public long getTamanoBytes() { return tamanoBytes; }
}
