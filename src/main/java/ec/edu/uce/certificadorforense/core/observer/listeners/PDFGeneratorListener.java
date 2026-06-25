package ec.edu.uce.certificadorforense.core.observer.listeners;

import ec.edu.uce.certificadorforense.core.observer.EventListener;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoFirmaRealizada;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoFirmaRealizada}, marca el contexto para que
 * {@code CertificacionState} sepa que puede proceder con la generación del PDF.
 * <p>
 * La generación real del PDF ocurre dentro de {@code CertificacionState.ejecutar()}.
 * Este listener actúa como señal de disparo.
 * </p>
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
        System.out.println("[PDFGeneratorListener] Firma completada — PDF listo para generarse en CertificacionState.");
        // La lógica de generación real se delega a CertificacionState.ejecutar()
        // para mantener el estado como único orquestador de su fase.
    }
}
