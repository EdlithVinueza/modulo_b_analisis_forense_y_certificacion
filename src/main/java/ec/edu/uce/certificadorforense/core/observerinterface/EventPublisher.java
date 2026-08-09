package ec.edu.uce.certificadorforense.core.observerinterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Publisher central del patrón Observer.
 */
public class EventPublisher {

    private final List<EventListener> listeners = new ArrayList<>();

    public void suscribir(EventListener listener) {
        listeners.add(listener);
    }

    public void desuscribir(EventListener listener) {
        listeners.remove(listener);
    }

    public void publicar(EventoSistema evento) {
        for (EventListener listener : listeners) {
            if (listener.soporta(evento)) {
                listener.onEvento(evento);
            }
        }
    }
}
