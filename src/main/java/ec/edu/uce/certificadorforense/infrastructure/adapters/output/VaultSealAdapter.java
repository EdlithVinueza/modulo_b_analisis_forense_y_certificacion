package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

@ApplicationScoped
public class VaultSealAdapter {

    private static final Logger log = Logger.getLogger(VaultSealAdapter.class);

    @Inject 
    SecretClient secretClient; // Cliente de Azure Key Vault

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public byte[] applyInstitutionalSeal(byte[] pdfReport, String caPassword) {
        try {
            log.info("Recuperando el sello institucional protegido en la nube de Azure...");
            // Recupera el secreto que está en Azure Key Vault
            KeyVaultSecret secret = secretClient.getSecret("system-certificadora-obras");
            byte[] p12Bytes = Base64.getDecoder().decode(secret.getValue());

            log.info("Aplicando sello institucional recuperado de HSM Azure directamente en memoria...");
            
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (ByteArrayInputStream bais = new ByteArrayInputStream(p12Bytes)) {
                ks.load(bais, caPassword.toCharArray());
            }

            Enumeration<String> aliases = ks.aliases();
            if (!aliases.hasMoreElements()) {
                throw new RuntimeException("El P12 de Azure no contiene ningún alias.");
            }
            String alias = aliases.nextElement();

            PrivateKey clavePrivada = (PrivateKey) ks.getKey(alias, caPassword.toCharArray());
            Certificate[] cadena = ks.getCertificateChain(alias);

            ((X509Certificate) cadena[0]).checkValidity();

            ByteArrayOutputStream baosFirmado = new ByteArrayOutputStream();
            try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfReport))) {
                PdfSigner signer = new PdfSigner(reader, baosFirmado, new StampingProperties());

                signer.setFieldName("FirmaInstitucionalVerisart");

                IExternalSignature firma = new PrivateKeySignature(
                        clavePrivada, DigestAlgorithms.SHA512,
                        BouncyCastleProvider.PROVIDER_NAME
                );
                IExternalDigest digest = new BouncyCastleDigest();

                signer.signDetached(digest, firma, cadena, null, null, null, 0,
                        PdfSigner.CryptoStandard.CMS);
            }

            log.info("Sello institucional aplicado con éxito en memoria.");
            return baosFirmado.toByteArray();

        } catch (Exception e) {
            log.error("Error al aplicar el sello institucional de Azure", e);
            throw new RuntimeException("Error al aplicar el sello institucional de Azure: " + e.getMessage(), e);
        }
    }
}
