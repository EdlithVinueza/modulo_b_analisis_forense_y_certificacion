package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

public class HashGeneratorListener implements EventListener {

    private final GeneradorHashPort generadorHash;
    private final ContextoProceso contexto;

    public HashGeneratorListener(GeneradorHashPort generadorHash, ContextoProceso contexto) {
        this.generadorHash = generadorHash;
        this.contexto = contexto;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (evento instanceof EventoAnalisisIniciado ev) {
            if (ev.getArchivoPSD() != null && ev.getArchivoPSD().getArchivo() != null) {
                try {
                    String hashPSD = generadorHash.calcularSHA512(ev.getArchivoPSD().getArchivo());
                    contexto.setSha512PSD(hashPSD);
                } catch (Exception ignored) {}
            }
            if (ev.getArchivoImagen() != null && ev.getArchivoImagen().getArchivo() != null) {
                try {
                    String hashImg = generadorHash.calcularSHA512(ev.getArchivoImagen().getArchivo());
                    contexto.setSha512Imagen(hashImg);
                } catch (Exception ignored) {}
            }
        }
    }
}
