package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistorialObserver implements EventListener {

    private final List<EventoSistema> historial = new ArrayList<>();

    @Override
    public void onEvento(EventoSistema evento) {
        historial.add(evento);
        System.out.println("[Auditoría] Evento registrado: " + evento.getNombreEvento() + " a las " + evento.getTimestamp());
    }

    public List<EventoSistema> getHistorial() {
        return Collections.unmodifiableList(historial);
    }
}
