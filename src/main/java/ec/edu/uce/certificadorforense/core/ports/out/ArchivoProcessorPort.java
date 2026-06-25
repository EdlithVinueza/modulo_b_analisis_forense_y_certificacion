package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import java.io.File;

public interface ArchivoProcessorPort<T extends ArchivoBase> {
    T procesar(File file);
    boolean soporta(File file);
}
