package ec.edu.uce.certificadorforense.core.model.base;

public interface Analizable {
    String getNombreArchivo();
    String getRutaAbsoluta();
    long getTamanoBytes();
    Object getMetadatos(); // Polimorfismo: cada uno devuelve sus propios metadatos
}
