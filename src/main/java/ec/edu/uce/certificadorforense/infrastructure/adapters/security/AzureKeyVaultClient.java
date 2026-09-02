package ec.edu.uce.certificadorforense.infrastructure.adapters.security;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Réplica del productor de CryptographyClient de Módulo A
 * (com.tesis.identity.infrastructure.security.AzureKeyVaultClient): mismo
 * vault, misma llave "master-custody-key". Necesario para que
 * VaultEncryptionService pueda desenvolver aquí la llave AES que Módulo A
 * envolvió allá — ambos módulos comparten la tabla "usuarios" y su cifrado.
 */
@ApplicationScoped
public class AzureKeyVaultClient {

    private static final Logger LOG = Logger.getLogger(AzureKeyVaultClient.class);

    @ConfigProperty(name = "tesis.azure.keyvault.url", defaultValue = "https://tesis-forensic-vault.vault.azure.net/")
    String vaultUrl;

    @ConfigProperty(name = "tesis.master-key.name", defaultValue = "master-custody-key")
    String keyName;

    @Produces
    @Dependent
    public CryptographyClient produceCryptographyClient() {
        try {
            var credential = resolveCredential();

            KeyClient keyClient = new KeyClientBuilder()
                    .vaultUrl(vaultUrl)
                    .credential(credential)
                    .buildClient();

            String keyId = keyClient.getKey(keyName).getId();

            return new CryptographyClientBuilder()
                    .keyIdentifier(keyId)
                    .credential(credential)
                    .buildClient();
        } catch (Exception e) {
            LOG.warn("[AzureKeyVaultClient] No se pudo conectar a Azure Key Vault (" + e.getMessage()
                    + "). Se usará el sobre criptográfico local, ver VaultEncryptionService.", e);
            return null;
        }
    }

    /** Ver AzureKeyVaultClient de Módulo A para el detalle del porqué del salto directo a Azure CLI en dev. */
    private TokenCredential resolveCredential() {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            return new AzureCliCredentialBuilder().processTimeout(Duration.ofSeconds(60)).build();
        }
        return new DefaultAzureCredentialBuilder().build();
    }
}
