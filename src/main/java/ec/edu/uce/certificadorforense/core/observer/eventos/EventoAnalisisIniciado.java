package ec.edu.uce.certificadorforense.core.observer.eventos;

import ec.edu.uce.certificadorforense.core.model.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import lombok.Getter;

import java.time.Instant;

/**
 * Evento disparado al cargar el PSD y la imagen en la Fase 1.
 * <p>
 * Los listeners suscritos a este evento ejecutan:
 * <ul>
 *   <li>{@code HashGeneratorListener} → calcula SHA512 PSD + imagen.</li>
 *   <li>{@code MetadataExtractorListener} → extrae metadatos.</li>
 *   <li>{@code CapasExtractorListener} → extrae capas PSD.</li>
 * </ul>
 * </p>
 */
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
