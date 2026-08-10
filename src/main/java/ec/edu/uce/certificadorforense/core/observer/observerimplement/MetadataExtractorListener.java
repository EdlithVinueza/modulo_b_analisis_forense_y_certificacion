package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

public class MetadataExtractorListener implements EventListener {

    private final ContextoProceso contexto;

    public MetadataExtractorListener(ContextoProceso contexto) {
        this.contexto = contexto;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (evento instanceof EventoAnalisisIniciado ev) {
            boolean tieneMetadatos = ev.getArchivoPSD() != null && ev.getArchivoPSD().getMetadatos() != null;
            contexto.setMetadatosDetectados(tieneMetadatos);
            System.out.println("[MetadataExtractorListener] Metadatos detectados: " + tieneMetadatos);
        }
    }
}
