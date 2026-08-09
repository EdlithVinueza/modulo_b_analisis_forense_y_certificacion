package ec.edu.uce.certificadorforense.infrastructure.adapters.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio Criptográfico de sobre para desencapsular la credencial P12 del usuario.
 */
@ApplicationScoped
public class VaultEncryptionService {

    private static final Logger log = Logger.getLogger(VaultEncryptionService.class);

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_BIT_LENGTH = 128;
    private static final int IV_BYTE_LENGTH = 12;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            byte[] iv = new byte[IV_BYTE_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] localMasterKey = "TESIS_LOCAL_MASTER_KEY_2026_SOBRE_512B_DEV_MODE!".getBytes(StandardCharsets.UTF_8);
            byte[] paddedKey = new byte[512];
            for (int i = 0; i < 512; i++) {
                paddedKey[i] = localMasterKey[i % localMasterKey.length];
            }
            byte[] rawKeyBytes = aesKey.getEncoded();
            byte[] encryptedAesKey = new byte[512];
            System.arraycopy(rawKeyBytes, 0, encryptedAesKey, 0, rawKeyBytes.length);
            for (int i = 0; i < 512; i++) {
                encryptedAesKey[i] ^= paddedKey[i];
            }

            byte[] combined = new byte[encryptedAesKey.length + iv.length + cipherText.length];
            System.arraycopy(encryptedAesKey, 0, combined, 0, encryptedAesKey.length);
            System.arraycopy(iv, 0, combined, encryptedAesKey.length, iv.length);
            System.arraycopy(cipherText, 0, combined, encryptedAesKey.length + iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Fallo en el sobre criptográfico: " + e.getMessage(), e);
            throw new RuntimeException("Error de seguridad en custodia híbrida: " + e.getMessage());
        }
    }

    public String decrypt(String combinedBase64) {
        if (combinedBase64 == null || combinedBase64.isEmpty()) return combinedBase64;

        // Si ya es un Base64 plano de PKCS12 (inicia con MII... o 3082...)
        if (combinedBase64.startsWith("MII") || combinedBase64.startsWith("3082")) {
            return combinedBase64;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(combinedBase64);

            int keyLength = 512;
            if (combined.length <= keyLength + IV_BYTE_LENGTH) {
                return combinedBase64;
            }

            byte[] encryptedAesKey = new byte[keyLength];
            byte[] iv = new byte[IV_BYTE_LENGTH];
            byte[] cipherText = new byte[combined.length - keyLength - IV_BYTE_LENGTH];

            System.arraycopy(combined, 0, encryptedAesKey, 0, keyLength);
            System.arraycopy(combined, keyLength, iv, 0, IV_BYTE_LENGTH);
            System.arraycopy(combined, keyLength + IV_BYTE_LENGTH, cipherText, 0, cipherText.length);

            byte[] localMasterKey = "TESIS_LOCAL_MASTER_KEY_2026_SOBRE_512B_DEV_MODE!".getBytes(StandardCharsets.UTF_8);
            byte[] paddedKey = new byte[512];
            for (int i = 0; i < 512; i++) {
                paddedKey[i] = localMasterKey[i % localMasterKey.length];
            }
            byte[] rawAes = new byte[32];
            byte[] unpadded = new byte[512];
            for (int i = 0; i < 512; i++) {
                unpadded[i] = (byte) (encryptedAesKey[i] ^ paddedKey[i]);
            }
            System.arraycopy(unpadded, 0, rawAes, 0, 32);

            SecretKey aesKey = new SecretKeySpec(rawAes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.warn("Formato no enmascarado o descifrado de sobre local: " + e.getMessage());
            return combinedBase64;
        }
    }
}
