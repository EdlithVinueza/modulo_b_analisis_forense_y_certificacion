package ec.edu.uce.certificadorforense.core.observer.listeners;

import ec.edu.uce.certificadorforense.core.observer.EventListener;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoAnalisisIniciado}, registra si se detectaron
 * metadatos en el PSD y lo almacena en el {@link ContextoProceso}.
 * <p>
 * Reutiliza los metadatos ya extraídos en {@code ArchivoPSD.getMetadatos()}.
 * </p>
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
        System.out.println("[MetadataExtractorListener] Verificando metadatos del PSD...");

        boolean tieneMetadatos = e.getArchivoPSD().getMetadatos() != null;
        contexto.setMetadatosDetectados(tieneMetadatos);

        System.out.println("  Metadatos detectados: " + tieneMetadatos);
    }
}
