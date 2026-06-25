package ec.edu.uce.certificadorforense.core.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Publisher central del patrón Observer.
 * <p>
 * Mantiene la lista de {@link EventListener} registrados y los notifica
 * cuando se publica un evento. Cada listener decide si lo maneja o no
 * mediante {@link EventListener#soporta(EventoSistema)}.
 * </p>
 *
 * <p>Uso:</p>
 * <pre>{@code
 * EventPublisher publisher = new EventPublisher();
 * publisher.suscribir(new HashGeneratorListener(...));
 * publisher.publicar(new EventoAnalisisIniciado(archivoPSD, archivoImagen));
 * }</pre>
 */
public class EventPublisher {

    private final List<EventListener> listeners = new ArrayList<>();

    /**
     * Registra un nuevo listener.
     *
     * @param listener El listener a suscribir.
     */
    public void suscribir(EventListener listener) {
        listeners.add(listener);
    }

    /**
     * Elimina un listener previamente registrado.
     *
     * @param listener El listener a eliminar.
     */
    public void desuscribir(EventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publica un evento a todos los listeners registrados que lo soporten.
     *
     * @param evento El evento a publicar.
     */
    public void publicar(EventoSistema evento) {
        System.out.println("[EventPublisher] Publicando evento: " + evento.getNombreEvento()
                + " @ " + evento.getTimestamp());
        for (EventListener listener : listeners) {
            if (listener.soporta(evento)) {
                listener.onEvento(evento);
            }
        }
    }
}
