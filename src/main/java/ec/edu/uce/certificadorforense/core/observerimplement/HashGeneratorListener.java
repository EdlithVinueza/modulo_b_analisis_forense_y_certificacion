package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

/**
 * Listener: al {@link EventoAnalisisIniciado}, calcula SHA-512 del PSD y la imagen
 * y los almacena en el {@link ContextoProceso}.
 */
public class HashGeneratorListener implements EventListener {

    private final GeneradorHashPort generadorHash;
    private final ContextoProceso contexto;

    public HashGeneratorListener(GeneradorHashPort generadorHash, ContextoProceso contexto) {
        this.generadorHash = generadorHash;
        this.contexto = contexto;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return evento instanceof EventoAnalisisIniciado;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        EventoAnalisisIniciado e = (EventoAnalisisIniciado) evento;
        try {
            byte[] bytesPSD    = e.getArchivoPSD().getContenidoBytes();
            byte[] bytesImagen = e.getArchivoImagen().getContenidoBytes();

            String sha512PSD = generadorHash.calcularSHA512(bytesPSD);
            String sha512Imagen = generadorHash.calcularSHA512(bytesImagen);

            contexto.setSha512PSD(sha512PSD);
            contexto.setSha512Imagen(sha512Imagen);
        } catch (Exception ex) {
            throw new RuntimeException("Error calculando hash SHA-512: " + ex.getMessage(), ex);
        }
    }
}
