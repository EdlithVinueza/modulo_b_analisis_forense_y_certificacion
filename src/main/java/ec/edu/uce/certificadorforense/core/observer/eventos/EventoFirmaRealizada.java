package ec.edu.uce.certificadorforense.core.observer.eventos;

import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import lombok.Getter;

import java.time.Instant;

/**
 * Evento disparado cuando el autor firma exitosamente el expediente en la Fase 3.
 * <p>
 * Los listeners suscritos a este evento ejecutan:
 * <ul>
 *   <li>{@code ExpedienteListener} → persiste el expediente-firmado.json.</li>
 *   <li>{@code AuditoriaListener} → registra el evento en el log de auditoría.</li>
 *   <li>{@code PDFGeneratorListener} → dispara la generación del PDF (Fase 4).</li>
 * </ul>
 * </p>
 */
@Getter
public class EventoFirmaRealizada implements EventoSistema {

    private final Expediente expediente;
    private final String expedienteJson;
    private final FirmaAutor firmaAutor;
    private final String timestamp;

    public EventoFirmaRealizada(Expediente expediente,
                                String expedienteJson,
                                FirmaAutor firmaAutor) {
        this.expediente = expediente;
        this.expedienteJson = expedienteJson;
        this.firmaAutor = firmaAutor;
        this.timestamp = Instant.now().toString();
    }

    @Override
    public String getNombreEvento() {
        return "FIRMA_REALIZADA";
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
