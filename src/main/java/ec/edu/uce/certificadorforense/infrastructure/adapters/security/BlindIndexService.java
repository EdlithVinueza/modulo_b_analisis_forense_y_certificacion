package ec.edu.uce.certificadorforense.infrastructure.adapters.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Réplica del índice ciego de cédula de Módulo A (mismo algoritmo, misma llave):
 * la tabla "usuarios" es compartida y desde que Módulo A cifra la columna
 * "cedula", la búsqueda ahí solo funciona por "cedula_hash". Debe usar
 * exactamente la misma normalización y el mismo secreto que
 * com.tesis.identity.infrastructure.security.BlindIndexService (Módulo A) o
 * los hashes no van a coincidir para la misma cédula.
 */
@ApplicationScoped
public class BlindIndexService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @ConfigProperty(name = "tesis.cedula.hmac-secret")
    String hmacSecret;

    public String hash(String plainText) {
        if (plainText == null) return null;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] result = mac.doFinal(plainText.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el índice ciego: " + e.getMessage(), e);
        }
    }
}
