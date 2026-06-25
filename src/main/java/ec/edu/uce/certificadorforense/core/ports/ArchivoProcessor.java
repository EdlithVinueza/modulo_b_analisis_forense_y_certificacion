package ec.edu.uce.certificadorforense.core.ports;

import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import java.io.File;

public interface ArchivoProcessor<T extends ArchivoBase> {
    T procesar(File file);
    boolean soporta(File file);
}
