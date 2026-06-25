package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida — Inyección de metadata de certificación en imágenes.
 * <p>
 * Implementaciones:
 * <ul>
 *   <li>PNG: inyecta un chunk {@code tEXt} personalizado antes del chunk {@code IEND}.</li>
 *   <li>JPEG: inyecta un segmento {@code APP11} (FF EB) con el JSON de certificación.</li>
 * </ul>
 * </p>
 */
public interface EsteganografiaPort {

    /**
     * Inyecta el JSON de certificación en la imagen.
     *
     * @param imagenOriginal Bytes de la imagen original (PNG o JPEG).
     * @param jsonCertificacion JSON a inyectar. Formato:
     *                          {@code {"id":"CERT-YYYY-NNNNNN","hash":"..."}}
     * @return Bytes de la imagen con el JSON inyectado.
     * @throws EsteganografiaException si el formato no es soportado o la inyección falla.
     */
    byte[] inyectar(byte[] imagenOriginal, String jsonCertificacion);

    /**
     * Extrae el JSON de certificación previamente inyectado en la imagen.
     *
     * @param imagenCertificada Bytes de la imagen certificada.
     * @return JSON extraído, o {@code null} si no tiene metadata inyectada.
     */
    String extraer(byte[] imagenCertificada);

    /**
     * Excepción específica para errores de esteganografía.
     */
    class EsteganografiaException extends RuntimeException {
        public EsteganografiaException(String mensaje) {
            super(mensaje);
        }
        public EsteganografiaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
