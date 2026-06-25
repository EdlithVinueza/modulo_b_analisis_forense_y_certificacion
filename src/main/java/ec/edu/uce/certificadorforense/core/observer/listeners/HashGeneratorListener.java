package ec.edu.uce.certificadorforense.core.observer.listeners;

import ec.edu.uce.certificadorforense.core.observer.EventListener;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoAnalisisIniciado;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import ec.edu.uce.certificadorforense.core.state.ContextoProceso;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        System.out.println("[HashGeneratorListener] Calculando SHA-512...");
        try {
            byte[] bytesPSD    = Files.readAllBytes(Paths.get(e.getArchivoPSD().getRutaAbsoluta()));
            byte[] bytesImagen = Files.readAllBytes(Paths.get(e.getArchivoImagen().getRutaAbsoluta()));

            String sha512PSD = generadorHash.calcularSHA512(bytesPSD);
            String sha512Imagen = generadorHash.calcularSHA512(bytesImagen);

            contexto.setSha512PSD(sha512PSD);
            contexto.setSha512Imagen(sha512Imagen);

            System.out.println("  SHA512 PSD:    " + sha512PSD);
            System.out.println("  SHA512 Imagen: " + sha512Imagen);
        } catch (IOException ex) {
            throw new RuntimeException("[HashGeneratorListener] Error leyendo archivos para hash: "
                    + ex.getMessage(), ex);
        }
    }
}
