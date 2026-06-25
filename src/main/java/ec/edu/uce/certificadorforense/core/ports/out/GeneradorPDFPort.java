package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;

/**
 * Puerto de salida — Generación del PDF visual del certificado.
 */
public interface GeneradorPDFPort {

    /**
     * Genera el PDF del certificado (sin firma digital aún).
     * <p>
     * El PDF incluye el certificado visual completo y el
     * {@code expediente-firmado.json} como archivo adjunto embebido.
     * </p>
     *
     * @param certificado      Datos del certificado emitido.
     * @param expediente       Datos completos del expediente.
     * @param expedienteJson   JSON serializado y firmado del expediente (se adjunta al PDF).
     * @param imagenBase64     Imagen de la obra en Base64 para la vista previa.
     * @return Bytes del PDF sin firmar, listo para pasar al {@link FirmadorPDFPort}.
     */
    byte[] generar(Certificado certificado,
                   Expediente expediente,
                   String expedienteJson,
                   String imagenBase64);
}
