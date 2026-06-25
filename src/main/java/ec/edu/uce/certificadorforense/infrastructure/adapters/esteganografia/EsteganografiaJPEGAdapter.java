package ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia;

import ec.edu.uce.certificadorforense.core.ports.out.EsteganografiaPort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador de infraestructura: inyección de JSON en imágenes JPEG/JPG.
 * <p>
 * Inserta un segmento {@code APP11} (marcador {@code 0xFF 0xEB}) reservado
 * exclusivamente para la certificación Verisart, separado del EXIF ({@code APP1}).
 * </p>
 *
 * <p>Formato del segmento JPEG insertado:</p>
 * <pre>
 * [FF EB] [2 bytes longitud total segmento] [datos UTF-8 del JSON]
 * </pre>
 *
 * <p>El segmento se inserta inmediatamente después del marcador SOI ({@code FF D8}).</p>
 */
public class EsteganografiaJPEGAdapter implements EsteganografiaPort {

    private static final byte MARKER_FF = (byte) 0xFF;
    private static final byte MARKER_EB = (byte) 0xEB; // APP11
    private static final byte MARKER_D8 = (byte) 0xD8; // SOI
    private static final String PREFIJO = "verisart-cert:";

    @Override
    public byte[] inyectar(byte[] imagenOriginal, String jsonCertificacion) {
        verificarFormatoJPEG(imagenOriginal);

        String contenido = PREFIJO + jsonCertificacion;
        byte[] datosContenido = contenido.getBytes(StandardCharsets.UTF_8);

        // Longitud del segmento: 2 bytes de longitud + datos (la longitud incluye los 2 bytes propios)
        int longitudSegmento = datosContenido.length + 2;
        if (longitudSegmento > 65533) {
            throw new EsteganografiaException("El JSON de certificación es demasiado largo para APP11.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // SOI original (FF D8)
            baos.write(imagenOriginal, 0, 2);

            // Segmento APP11 (FF EB + longitud + datos)
            baos.write(MARKER_FF);
            baos.write(MARKER_EB);
            baos.write((longitudSegmento >> 8) & 0xFF);
            baos.write(longitudSegmento & 0xFF);
            baos.write(datosContenido);

            // Resto del JPEG original (sin el SOI)
            baos.write(imagenOriginal, 2, imagenOriginal.length - 2);

        } catch (Exception e) {
            throw new EsteganografiaException("Error inyectando segmento APP11 en JPEG: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    @Override
    public String extraer(byte[] imagenCertificada) {
        verificarFormatoJPEG(imagenCertificada);

        int pos = 2; // saltar SOI
        while (pos + 4 <= imagenCertificada.length) {
            if ((imagenCertificada[pos] & 0xFF) != 0xFF) break;
            int marcador = imagenCertificada[pos + 1] & 0xFF;
            int longitud = ((imagenCertificada[pos + 2] & 0xFF) << 8)
                    | (imagenCertificada[pos + 3] & 0xFF);

            if (marcador == 0xEB && longitud > PREFIJO.length() + 2) {
                byte[] datos = new byte[longitud - 2];
                System.arraycopy(imagenCertificada, pos + 4, datos, 0, datos.length);
                String texto = new String(datos, StandardCharsets.UTF_8);
                if (texto.startsWith(PREFIJO)) {
                    return texto.substring(PREFIJO.length());
                }
            }
            pos += 2 + longitud;
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void verificarFormatoJPEG(byte[] datos) {
        if (datos == null || datos.length < 4) {
            throw new EsteganografiaException("Los datos no son un JPEG válido.");
        }
        if ((datos[0] & 0xFF) != 0xFF || (datos[1] & 0xFF) != 0xD8) {
            throw new EsteganografiaException("Firma JPEG inválida (SOI no encontrado). ¿Es realmente un JPEG?");
        }
    }
}
