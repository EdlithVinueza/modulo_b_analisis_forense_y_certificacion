package ec.edu.uce.certificadorforense.infrastructure.adapters.hash;

import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;

/**
 * Adaptador de infraestructura: cálculo SHA-512 con {@code java.security.MessageDigest}.
 * Sin dependencias externas.
 */
@ApplicationScoped
public class SHA512Adapter implements GeneradorHashPort {

    @Override
    public String calcularSHA512(byte[] datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(datos);
            return bytesAHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 no disponible en la JVM", e);
        }
    }

    @Override
    public String calcularSHA512(String texto) {
        return calcularSHA512(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    @Override
    public String calcularSHA512(File archivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (FileInputStream fis = new FileInputStream(archivo);
                 DigestInputStream dis = new DigestInputStream(fis, digest)) {
                
                byte[] buffer = new byte[65536]; // 64 KB buffer para lectura masiva eficiente
                while (dis.read(buffer) != -1) {
                    // El DigestInputStream actualiza el MessageDigest internamente
                }
            }
            byte[] hashBytes = digest.digest();
            return bytesAHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 no disponible en la JVM", e);
        }
    }

    private String bytesAHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
