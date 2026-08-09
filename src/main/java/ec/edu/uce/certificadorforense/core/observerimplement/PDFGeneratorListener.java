package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoFirmaRealizada;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoFirmaRealizada}, marca el contexto para que
 * {@code CertificacionState} sepa que puede proceder con la generación del PDF.
 */
public class PDFGeneratorListener implements EventListener {

    private final ContextoProceso contexto;

    public PDFGeneratorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return evento instanceof EventoFirmaRealizada;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        // La lógica de generación real se delega a CertificacionState.ejecutar()
    }
}
