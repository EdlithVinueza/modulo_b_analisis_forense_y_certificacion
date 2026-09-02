package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import ec.edu.uce.certificadorforense.core.ports.out.SelladorInstitucionalPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorPDFAdapter;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Base64;

/**
 * Adaptador de Infraestructura para el sellado institucional de PDFs.
 * Implementa {@link SelladorInstitucionalPort} con el certificado
 * "system-certificadora-obras" de Azure Key Vault: el secreto vinculado a
 * ese Key Vault Certificate entrega el PKCS#12 completo (privada + cadena)
 * directamente en memoria — no hay archivo local ni ruta hardcodeada.
 */
@ApplicationScoped
public class VaultSealAdapter implements SelladorInstitucionalPort {

    private static final Logger LOG = Logger.getLogger(VaultSealAdapter.class);

    @ConfigProperty(name = "tesis.azure.keyvault.url")
    String vaultUrl;

    @ConfigProperty(name = "tesis.cert.name", defaultValue = "system-certificadora-obras")
    String certName;

    @Override
    public byte[] sellar(byte[] pdfBytes, String password) {
        try {
            var credential = resolveCredential();
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(vaultUrl)
                    .credential(credential)
                    .buildClient();

            String secretValue = secretClient.getSecret(certName).getValue();
            byte[] pkcs12Bytes = Base64.getDecoder().decode(secretValue);

            FirmadorPDFAdapter firmador = new FirmadorPDFAdapter(pkcs12Bytes);
            return firmador.firmarPDF(pdfBytes, password);
        } catch (Exception e) {
            // No degradamos a "PDF sin sellar": un certificado sin el sello
            // institucional no es un certificado válido, así que esto debe
            // fallar la operación completa, no reportarse como éxito.
            LOG.error("[VaultSealAdapter] No se pudo sellar con el certificado institucional de Key Vault: "
                    + e.getMessage(), e);
            throw new SelloException(
                    "No se pudo aplicar el sello institucional al certificado. Intenta nuevamente en unos minutos.", e);
        }
    }

    /** Ver AzureKeyVaultClient para el detalle del porqué del salto directo a Azure CLI en dev. */
    private TokenCredential resolveCredential() {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            return new AzureCliCredentialBuilder().processTimeout(Duration.ofSeconds(60)).build();
        }
        return new DefaultAzureCredentialBuilder().build();
    }
}
