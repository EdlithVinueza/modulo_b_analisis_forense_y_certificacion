package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class NumerosMagicos {  // Firmas hexadecimales
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PSD_SIGNATURE = {0x38, 0x42, 0x50, 0x53}; // "8BPS"

    public static String detectarFormatoReal(File archivo) {
        byte[] encabezado = new byte[8];
        try (FileInputStream fis = new FileInputStream(archivo)) {
            if (fis.read(encabezado) < 4) return "DESCONOCIDO";

            if (compararBytes(encabezado, PSD_SIGNATURE, 4)) return "PSD";
            if (compararBytes(encabezado, PNG_SIGNATURE, 8)) return "PNG";
            if (compararBytes(encabezado, JPEG_SIGNATURE, 3)) return "JPEG";

        } catch (IOException e) {
            return "ERROR_LECTURA";
        }
        return "OTRO";
    }

    private static boolean compararBytes(byte[] a, byte[] b, int n) {
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
