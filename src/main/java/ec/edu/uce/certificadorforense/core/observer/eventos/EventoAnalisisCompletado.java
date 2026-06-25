package ec.edu.uce.certificadorforense.core.observer.eventos;

import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import lombok.Getter;

import java.time.Instant;

/**
 * Evento disparado cuando el análisis forense de la Fase 1 concluye con resultado APROBADO.
 */
@Getter
public class EventoAnalisisCompletado implements EventoSistema {

    private final String sha512PSD;
    private final String sha512Imagen;
    private final String pHash;
    private final int capasPSD;
    private final String timestamp;

    public EventoAnalisisCompletado(String sha512PSD, String sha512Imagen,
                                    String pHash, int capasPSD) {
        this.sha512PSD = sha512PSD;
        this.sha512Imagen = sha512Imagen;
        this.pHash = pHash;
        this.capasPSD = capasPSD;
        this.timestamp = Instant.now().toString();
    }

    @Override
    public String getNombreEvento() {
        return "ANALISIS_COMPLETADO";
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
