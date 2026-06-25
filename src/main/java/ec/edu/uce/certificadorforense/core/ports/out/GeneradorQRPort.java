package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida — Generación de código QR.
 */
public interface GeneradorQRPort {

    /**
     * Genera una imagen PNG de un código QR con el contenido dado.
     * <p>
     * Para el sistema Verisart (Opción A), el contenido es el ID interno:
     * {@code CERT-NNNNNN}.
     * </p>
     *
     * @param contenido Texto a codificar en el QR (ej. {@code "CERT-000001"}).
     * @param ancho     Ancho en píxeles del QR generado.
     * @param alto      Alto en píxeles del QR generado.
     * @return Bytes PNG de la imagen del QR.
     */
    byte[] generar(String contenido, int ancho, int alto);
}
