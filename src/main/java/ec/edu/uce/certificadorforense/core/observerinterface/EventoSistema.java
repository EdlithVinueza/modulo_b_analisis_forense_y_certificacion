package ec.edu.uce.certificadorforense.core.observerinterface;

/**
 * Interfaz marker base para todos los eventos del sistema.
 */
public interface EventoSistema {

    String getNombreEvento();

    String getTimestamp();
}
