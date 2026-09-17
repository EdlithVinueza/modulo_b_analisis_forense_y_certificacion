package ec.edu.uce.certificadorforense.infrastructure.adapters.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.util.Optional;

/**
 * Health check de preparación (Readiness) según el estándar MicroProfile Health para Módulo B.
 * Verifica la presencia y configuración del endpoint de Azure Key Vault.
 */
@Readiness
@ApplicationScoped
public class KeyVaultReadinessCheck implements HealthCheck {

    @ConfigProperty(name = "tesis.azure.keyvault.url")
    Optional<String> vaultUrl;

    @ConfigProperty(name = "tesis.master-key.name", defaultValue = "master-custody-key")
    String masterKeyName;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("azure-keyvault-readiness");

        if (vaultUrl.isPresent() && !vaultUrl.get().isBlank()) {
            return builder.up()
                    .withData("vaultUrl", vaultUrl.get())
                    .withData("masterKeyName", masterKeyName)
                    .build();
        } else {
            return builder.down()
                    .withData("error", "URL de Azure Key Vault no configurada")
                    .build();
        }
    }
}
