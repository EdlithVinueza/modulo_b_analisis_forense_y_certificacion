package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoAnalisisIniciado}, extrae el número de capas del PSD
 * y lo almacena en el {@link ContextoProceso}.
 */
public class CapasExtractorListener implements EventListener {

    private final ContextoProceso contexto;

    public CapasExtractorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return evento instanceof EventoAnalisisIniciado;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        EventoAnalisisIniciado e = (EventoAnalisisIniciado) evento;
        int numCapas = e.getArchivoPSD().getCapas() != null
                ? e.getArchivoPSD().getCapas().size()
                : 0;
        contexto.setCapasPSD(numCapas);
    }
}
