package ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia;

import ec.edu.uce.certificadorforense.core.ports.out.EsteganografiaPort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Adaptador de infraestructura: inyección de JSON en imágenes PNG.
 * <p>
 * Inserta un chunk {@code tEXt} personalizado con clave {@code "verisart-cert"}
 * justo antes del chunk {@code IEND} de cierre del PNG, sin modificar los
 * píxeles ({@code IDAT}) ni la estructura visible de la imagen.
 * </p>
 *
 * <p>Formato del chunk PNG insertado:</p>
 * <pre>
 * [4 bytes longitud datos] [4 bytes "tEXt"] [keyword\0value] [4 bytes CRC32]
 * </pre>
 */
public class EsteganografiaPNGAdapter implements EsteganografiaPort {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] CHUNK_IEND = {0x49, 0x45, 0x4E, 0x44};
    private static final String KEYWORD = "verisart-cert";

    @Override
    public byte[] inyectar(byte[] imagenOriginal, String jsonCertificacion) {
        verificarFormatoPNG(imagenOriginal);

        // Construir el nuevo chunk tEXt
        byte[] chunkData = construirChunkTexto(jsonCertificacion);

        // Localizar posición del chunk IEND (últimos 12 bytes del PNG)
        int posicionIEND = buscarPosicionIEND(imagenOriginal);

        // Ensamblar: todo antes de IEND + nuevo chunk + IEND
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(imagenOriginal, 0, posicionIEND);
            baos.write(chunkData);
            baos.write(imagenOriginal, posicionIEND, imagenOriginal.length - posicionIEND);
        } catch (Exception e) {
            throw new EsteganografiaException("Error ensamblando PNG certificado: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    @Override
    public String extraer(byte[] imagenCertificada) {
        verificarFormatoPNG(imagenCertificada);
        String keyword = KEYWORD + "\0";
        byte[] keywordBytes = keyword.getBytes(StandardCharsets.ISO_8859_1);

        int pos = 8; // saltar firma PNG
        while (pos + 12 <= imagenCertificada.length) {
            int longitud = leerInt(imagenCertificada, pos);
            String tipo = new String(imagenCertificada, pos + 4, 4, StandardCharsets.ISO_8859_1);

            if ("tEXt".equals(tipo) && longitud > keywordBytes.length) {
                byte[] datos = Arrays.copyOfRange(imagenCertificada, pos + 8, pos + 8 + longitud);
                String texto = new String(datos, StandardCharsets.ISO_8859_1);
                if (texto.startsWith(KEYWORD + "\0")) {
                    return texto.substring(KEYWORD.length() + 1);
                }
            }
            pos += 12 + longitud;
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private byte[] construirChunkTexto(String json) {
        String contenido = KEYWORD + "\0" + json;
        byte[] datos = contenido.getBytes(StandardCharsets.ISO_8859_1);

        CRC32 crc = new CRC32();
        crc.update(CHUNK_IEND); // reutilizamos el array "tEXt"
        byte[] tipoBytes = "tEXt".getBytes(StandardCharsets.ISO_8859_1);
        crc.reset();
        crc.update(tipoBytes);
        crc.update(datos);
        long crcValor = crc.getValue();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(intToBytes(datos.length));
            baos.write(tipoBytes);
            baos.write(datos);
            baos.write(intToBytes((int) crcValor));
        } catch (Exception e) {
            throw new EsteganografiaException("Error construyendo chunk tEXt", e);
        }
        return baos.toByteArray();
    }

    private int buscarPosicionIEND(byte[] png) {
        for (int i = png.length - 12; i >= 8; i--) {
            if (png[i + 4] == 'I' && png[i + 5] == 'E'
                    && png[i + 6] == 'N' && png[i + 7] == 'D') {
                return i;
            }
        }
        throw new EsteganografiaException("No se encontró el chunk IEND en el PNG.");
    }

    private void verificarFormatoPNG(byte[] datos) {
        if (datos == null || datos.length < 8) {
            throw new EsteganografiaException("Los datos no son un PNG válido.");
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (datos[i] != PNG_SIGNATURE[i]) {
                throw new EsteganografiaException("Firma PNG inválida. ¿Es realmente un PNG?");
            }
        }
    }

    private int leerInt(byte[] datos, int offset) {
        return ((datos[offset] & 0xFF) << 24) | ((datos[offset + 1] & 0xFF) << 16)
                | ((datos[offset + 2] & 0xFF) << 8) | (datos[offset + 3] & 0xFF);
    }

    private byte[] intToBytes(int valor) {
        return new byte[]{
            (byte) (valor >> 24), (byte) (valor >> 16),
            (byte) (valor >> 8), (byte) valor
        };
    }
}
