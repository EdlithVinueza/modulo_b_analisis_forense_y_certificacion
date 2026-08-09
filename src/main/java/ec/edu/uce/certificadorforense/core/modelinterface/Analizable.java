package ec.edu.uce.certificadorforense.core.modelinterface;

public interface Analizable {
    String getNombreArchivo();
    long getTamanoBytes();
    Object getMetadatos(); // Polimorfismo: cada uno devuelve sus propios metadatos
}
