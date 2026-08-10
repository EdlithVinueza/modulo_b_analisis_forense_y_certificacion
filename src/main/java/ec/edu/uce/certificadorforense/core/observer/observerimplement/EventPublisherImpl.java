package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventPublisher;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;

import java.util.ArrayList;
import java.util.List;

public class EventPublisherImpl implements EventPublisher {

    private final List<EventListener> listeners = new ArrayList<>();

    @Override
    public void suscribir(EventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void desuscribir(EventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void publicar(EventoSistema evento) {
        for (EventListener listener : listeners) {
            listener.onEvento(evento);
        }
    }
}
