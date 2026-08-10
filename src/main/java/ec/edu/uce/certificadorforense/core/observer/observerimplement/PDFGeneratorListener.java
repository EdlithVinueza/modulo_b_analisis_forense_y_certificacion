package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoFirmaRealizada;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

public class PDFGeneratorListener implements EventListener {

    private final ContextoProceso contexto;

    public PDFGeneratorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (evento instanceof EventoFirmaRealizada ev) {
            System.out.println("[PDFGeneratorListener] Evento FirmaRealizada recibido para expediente: "
                    + ev.getExpediente().getIdExpediente());
        }
    }
}
