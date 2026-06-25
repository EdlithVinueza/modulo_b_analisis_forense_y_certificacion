package ec.edu.uce.certificadorforense.core.model.certificado;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Representa el certificado de autenticidad digital emitido en la Fase 4.
 * <p>
 * Lo genera la entidad (sistema), no el autor.
 * Contiene el identificador oficial, el QR y los vínculos al expediente firmado.
 * </p>
 */
@Getter
@Builder
@ToString
public class Certificado {

    /**
     * Identificador único del certificado.
     * Formato: {@code CERT-YYYY-NNNNNN} (ej. {@code CERT-2026-000001}).
     */
    private final String idCertificado;

    /**
     * Identificador del expediente asociado.
     * Formato: {@code EXP-YYYY-NNNNNN}.
     */
    private final String idExpediente;

    /** Fecha y hora de emisión del certificado (UTC). */
    private final Instant fechaEmision;

    /** SHA-512 del expediente firmado (JSON + firma autor). Garantiza integridad. */
    private final String hashExpedienteFirmado;

    /**
     * Contenido exacto del código QR.
     * Opción A — solo el ID interno: {@code CERT-NNNNNN}.
     */
    private final String qrContenido;

    /** Imagen del QR codificada en Base64 (PNG). */
    private final String qrBase64;
}
