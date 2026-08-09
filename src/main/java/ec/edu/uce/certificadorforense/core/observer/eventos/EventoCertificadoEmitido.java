package ec.edu.uce.certificadorforense.core.observer.eventos;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
import lombok.Getter;

import java.time.Instant;

/**
 * Evento disparado cuando la Fase 4 genera el PDF firmado y la imagen certificada.
 * Este es el evento final del flujo de certificación.
 */
@Getter
public class EventoCertificadoEmitido implements EventoSistema {

    private final Certificado certificado;
    private final String idCertificado;
    private final String timestamp;

    public EventoCertificadoEmitido(Certificado certificado) {
        this.certificado = certificado;
        this.idCertificado = certificado.getIdCertificado();
        this.timestamp = Instant.now().toString();
    }

    @Override
    public String getNombreEvento() {
        return "CERTIFICADO_EMITIDO";
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
