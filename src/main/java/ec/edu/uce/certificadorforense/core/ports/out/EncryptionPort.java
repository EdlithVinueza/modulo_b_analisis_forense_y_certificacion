package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida para el cifrado y descifrado de datos sensibles (envelope encryption con Azure Key Vault).
 * Permite que los casos de uso dependan de una abstracción en lugar de la infraestructura directa.
 */
public interface EncryptionPort {

    String encrypt(String plainText);

    String decrypt(String combinedBase64);
}
