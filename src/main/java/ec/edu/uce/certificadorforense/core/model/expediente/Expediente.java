package ec.edu.uce.certificadorforense.core.model.expediente;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Expediente completo del proceso de certificación.
 * <p>
 * Es el documento principal que se serializa a JSON y se firma digitalmente
 * por el autor en la Fase 3. Contiene todos los datos recopilados en las
 * Fases 1 y 2.
 * </p>
 *
 * <p>Estructura JSON resultante:</p>
 * <pre>{@code
 * {
 *   "idExpediente": "EXP-2026-000001",
 *   "fechaRegistro": "2026-06-21T18:00:00Z",
 *   "autor": { ... },
 *   "obra":  { ... },
 *   "analisis": { "resultado": "", "capasPSD": 0, "metadatosDetectados": true },
 *   "hashes": { "sha512PSD": "", "sha512Imagen": "", "pHash": "" }
 * }
 * }</pre>
 */
@Getter
@Builder
@ToString
public class Expediente {

    /** Identificador único del expediente. Formato: {@code EXP-YYYY-NNNNNN}. */
    private final String idExpediente;

    /** Fecha y hora de registro en formato ISO-8601 UTC. */
    private final String fechaRegistro;

    /** Datos del autor de la obra. */
    private final Autor autor;

    /** Datos descriptivos de la obra. */
    private final Obra obra;

    /** Resumen del análisis forense realizado en Fase 1. */
    private final AnalisisResumen analisis;

    /** Huellas criptográficas y perceptuales de los archivos de evidencia. */
    private final HashesEvidencia hashes;


}
