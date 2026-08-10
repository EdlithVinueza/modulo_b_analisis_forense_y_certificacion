package ec.edu.uce.certificadorforense.core.model.modelinterface;

import java.io.File;

public interface Analizable {
    File getArchivo();
    String getNombreArchivo();
    long getTamanoBytes();
}
