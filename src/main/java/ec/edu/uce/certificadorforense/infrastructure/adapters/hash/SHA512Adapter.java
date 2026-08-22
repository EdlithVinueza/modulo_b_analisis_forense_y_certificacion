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

    @Override
    public String calcularSHA512(File archivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (FileInputStream fis = new FileInputStream(archivo);
                 DigestInputStream dis = new DigestInputStream(fis, digest)) {
                
                byte[] buffer = new byte[8192];
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
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
