package ec.edu.uce.certificadorforense.infrastructure.adapters.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorQRPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador de infraestructura: generación de QR usando ZXing.
 * <p>
 * Genera un PNG del QR con el contenido dado (Opción A: ID interno {@code CERT-NNNNNN}).
 * </p>
 */
public class QRGeneratorAdapter implements GeneradorQRPort {

    @Override
    public byte[] generar(String contenido, int ancho, int alto) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto, hints);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();

        } catch (WriterException | IOException e) {
            throw new RuntimeException("[QRGeneratorAdapter] Error generando QR para: " + contenido, e);
        }
    }
}
