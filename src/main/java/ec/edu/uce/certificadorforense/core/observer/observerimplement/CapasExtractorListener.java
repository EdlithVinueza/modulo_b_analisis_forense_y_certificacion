package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

public class CapasExtractorListener implements EventListener {

    private final ContextoProceso contexto;

    public CapasExtractorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (evento instanceof EventoAnalisisIniciado ev) {
            if (ev.getArchivoPSD() != null) {
                int numCapas = ev.getArchivoPSD().getCapas().size();
                contexto.setCapasPSD(numCapas);
                System.out.println("[CapasExtractorListener] Capas detectadas en PSD: " + numCapas);
            }
        }
    }
}
