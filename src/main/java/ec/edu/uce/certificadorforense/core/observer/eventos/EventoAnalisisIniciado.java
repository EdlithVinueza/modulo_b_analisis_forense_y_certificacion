package ec.edu.uce.certificadorforense.core.observer.eventos;

import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import lombok.Getter;

import java.time.Instant;

@Getter
public class EventoAnalisisIniciado implements EventoSistema {

    private final ArchivoPSD archivoPSD;
    private final ArchivoImagen archivoImagen;
    private final String timestamp;

    public EventoAnalisisIniciado(ArchivoPSD archivoPSD, ArchivoImagen archivoImagen) {
        this.archivoPSD = archivoPSD;
        this.archivoImagen = archivoImagen;
        this.timestamp = Instant.now().toString();
    }

    @Override
    public String getNombreEvento() {
        return "ANALISIS_INICIADO";
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
