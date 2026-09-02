package ec.edu.uce.certificadorforense.infrastructure.adapters.security;

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Envelope encryption — réplica de
 * com.tesis.identity.infrastructure.security.VaultEncryptionService (Módulo A).
 * Debe desenvolver la llave AES con la MISMA llave Key Vault que usó A para
 * envolverla (nombres/apellidos/cédula en la tabla "usuarios" compartida) o
 * cualquier variación en el fallback local para la credencial P12 propia de B.
 */
@ApplicationScoped
public class VaultEncryptionService {

    private static final Logger log = Logger.getLogger(VaultEncryptionService.class);

    @Inject
    CryptographyClient cryptoClient;

    @ConfigProperty(name = "tesis.encryption.allow-insecure-fallback", defaultValue = "true")
    boolean allowInsecureFallback;

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_BIT_LENGTH = 128;
    private static final int IV_BYTE_LENGTH = 12;
    private static final int WRAPPED_KEY_LENGTH = 512; // RSA-4096 => bloque de 512 bytes

    private static final byte[] LOCAL_MASTER_KEY_PADDED = buildPaddedLocalMasterKey();

    private static byte[] buildPaddedLocalMasterKey() {
        byte[] localMasterKey = "TESIS_LOCAL_MASTER_KEY_2026_SOBRE_512B_DEV_MODE!".getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[WRAPPED_KEY_LENGTH];
        for (int i = 0; i < WRAPPED_KEY_LENGTH; i++) {
            padded[i] = localMasterKey[i % localMasterKey.length];
        }
        return padded;
    }

    private static byte[] xorWithLocalMasterKey(byte[] data) {
        byte[] result = new byte[WRAPPED_KEY_LENGTH];
        for (int i = 0; i < WRAPPED_KEY_LENGTH; i++) {
            result[i] = (byte) (data[i] ^ LOCAL_MASTER_KEY_PADDED[i]);
        }
        return result;
    }

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

            byte[] encryptedAesKey;
            try {
                if (cryptoClient != null) {
                    encryptedAesKey = cryptoClient.encrypt(EncryptionAlgorithm.RSA_OAEP_256, aesKey.getEncoded()).getCipherText();
                } else {
                    throw new IllegalStateException("cryptoClient no inicializado");
                }
            } catch (Exception azureEx) {
                encryptedAesKey = localFallbackWrap(aesKey.getEncoded(), azureEx);
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

        // Si ya es un Base64 plano de PKCS12 (inicia con MII... o 3082...), no está envuelto.
        if (combinedBase64.startsWith("MII") || combinedBase64.startsWith("3082")) {
            return combinedBase64;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(combinedBase64);

            if (combined.length <= WRAPPED_KEY_LENGTH + IV_BYTE_LENGTH) {
                return combinedBase64;
            }

            byte[] encryptedAesKey = new byte[WRAPPED_KEY_LENGTH];
            byte[] iv = new byte[IV_BYTE_LENGTH];
            byte[] cipherText = new byte[combined.length - WRAPPED_KEY_LENGTH - IV_BYTE_LENGTH];

            System.arraycopy(combined, 0, encryptedAesKey, 0, WRAPPED_KEY_LENGTH);
            System.arraycopy(combined, WRAPPED_KEY_LENGTH, iv, 0, IV_BYTE_LENGTH);
            System.arraycopy(combined, WRAPPED_KEY_LENGTH + IV_BYTE_LENGTH, cipherText, 0, cipherText.length);

            byte[] decryptedAesKey;
            try {
                if (cryptoClient != null) {
                    decryptedAesKey = cryptoClient.decrypt(EncryptionAlgorithm.RSA_OAEP_256, encryptedAesKey).getPlainText();
                } else {
                    throw new IllegalStateException("cryptoClient no inicializado");
                }
            } catch (Exception azureEx) {
                decryptedAesKey = localFallbackUnwrap(encryptedAesKey, azureEx);
            }

            SecretKey aesKey = new SecretKeySpec(decryptedAesKey, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.warn("Formato no enmascarado o descifrado de sobre local: " + e.getMessage());
            return combinedBase64;
        }
    }

    private byte[] localFallbackWrap(byte[] rawAesKey, Exception azureEx) {
        requireFallbackAllowed(azureEx, "encrypt");
        byte[] padded = new byte[WRAPPED_KEY_LENGTH];
        System.arraycopy(rawAesKey, 0, padded, 0, rawAesKey.length);
        return xorWithLocalMasterKey(padded);
    }

    private byte[] localFallbackUnwrap(byte[] wrappedAesKey, Exception azureEx) {
        requireFallbackAllowed(azureEx, "decrypt");
        byte[] unpadded = xorWithLocalMasterKey(wrappedAesKey);
        return Arrays.copyOf(unpadded, 32); // AES-256 -> 32 bytes
    }

    private void requireFallbackAllowed(Exception azureEx, String operacion) {
        if (!allowInsecureFallback) {
            throw new IllegalStateException(
                    "Azure Key Vault no disponible y el fallback de cifrado local está deshabilitado "
                            + "(tesis.encryption.allow-insecure-fallback=false).", azureEx);
        }
        log.warn("[VaultEncryptionService] ALERTA DE SEGURIDAD (" + operacion + "): Azure Key Vault no disponible, "
                + "aplicando sobre criptográfico LOCAL INSEGURO (fallback temporal). Causa: " + azureEx.getMessage());
    }
}
