package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import ec.edu.uce.certificadorforense.core.ports.out.SelladorInstitucionalPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorPDFAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
            var credential = new DefaultAzureCredentialBuilder().build();
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(vaultUrl)
                    .credential(credential)
                    .buildClient();

            String secretValue = secretClient.getSecret(certName).getValue();
            byte[] pkcs12Bytes = Base64.getDecoder().decode(secretValue);

            FirmadorPDFAdapter firmador = new FirmadorPDFAdapter(pkcs12Bytes);
            return firmador.firmarPDF(pdfBytes, password);
        } catch (Exception e) {
            LOG.warn("[VaultSealAdapter] No se pudo sellar con el certificado institucional de Key Vault ("
                    + e.getMessage() + "). Se retorna el PDF sin sellar.", e);
            return pdfBytes;
        }
    }
}
