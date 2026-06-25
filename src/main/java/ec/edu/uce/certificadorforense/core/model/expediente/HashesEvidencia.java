package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Huellas criptográficas y perceptuales de los archivos de evidencia.
 * Forma parte del expediente JSON que se firma en Fase 3.
 */
@Getter
@Builder
@ToString
public class HashesEvidencia {

    /** SHA-512 del archivo PSD original. */
    private final String sha512PSD;

    /** SHA-512 de la imagen exportada (PNG/JPG). */
    private final String sha512Imagen;

    /** Hash perceptual (pHash) de la imagen exportada. */
    private final String pHash;
}
