package ec.edu.uce.certificadorforense.core.model.firma;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Representa la firma digital del autor sobre el expediente JSON.
 * <p>
 * El autor firma el expediente con su propio certificado P12 en la Fase 3,
 * declarando que la información es correcta y que está registrando la obra.
 * </p>
 */
@Getter
@Builder
@ToString
public class FirmaAutor {

    /** SHA-512 del JSON del expediente antes de firmar. */
    private final String hashExpediente;

    /** Firma digital en Base64 del expediente (SHA512withRSA). */
    private final String firmaBase64;

    /** Algoritmo de firma utilizado. */
    @Builder.Default
    private final String algoritmo = "SHA512withRSA";

    /** Instante en que se realizó la firma. */
    private final Instant fechaFirma;

    /** Nombre del alias dentro del keystore P12 utilizado. */
    private final String aliasKeystore;
}
