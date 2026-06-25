package ec.edu.uce.certificadorforense.core.observer;

/**
 * Interfaz marker base para todos los eventos del sistema Verisart.
 * <p>
 * Todos los eventos concretos implementan esta interfaz para poder ser
 * publicados y recibidos de forma genérica por el {@link EventPublisher}.
 * </p>
 */
public interface EventoSistema {

    /**
     * Nombre descriptivo del evento (para logging y auditoría).
     *
     * @return Nombre del evento.
     */
    String getNombreEvento();

    /**
     * Instante en que ocurrió el evento (ISO-8601 UTC).
     *
     * @return Timestamp del evento.
     */
    String getTimestamp();
}
