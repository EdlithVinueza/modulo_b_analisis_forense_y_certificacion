package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoAnalisisIniciado}, registra si se detectaron
 * metadatos en el PSD y lo almacena en el {@link ContextoProceso}.
 */
public class MetadataExtractorListener implements EventListener {

    private final ContextoProceso contexto;

    public MetadataExtractorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return evento instanceof EventoAnalisisIniciado;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        EventoAnalisisIniciado e = (EventoAnalisisIniciado) evento;
        boolean tieneMetadatos = e.getArchivoPSD().getMetadatos() != null;
        contexto.setMetadatosDetectados(tieneMetadatos);
    }
}
