package ec.edu.uce.certificadorforense.infrastructure.adapters.firma;

import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import ec.edu.uce.certificadorforense.core.ports.out.FirmadorPDFPort;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Adaptador de infraestructura: firma digital del PDF con el certificado
 * institucional cargado desde Azure Key Vault (ver VaultSealAdapter).
 * <p>
 * Usa iText 7 {@code PdfSigner} con el proveedor criptográfico BouncyCastle.
 * </p>
 */
public class FirmadorPDFAdapter implements FirmadorPDFPort {

    private final byte[] pkcs12Bytes;

    public FirmadorPDFAdapter(byte[] pkcs12Bytes) {
        this.pkcs12Bytes = pkcs12Bytes;
    }

    static {
        // Registrar BouncyCastle como proveedor de seguridad
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public byte[] firmarPDF(byte[] pdfSinFirmar, String contrasenaCA) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (ByteArrayInputStream bis = new ByteArrayInputStream(pkcs12Bytes)) {
                ks.load(bis, contrasenaCA.toCharArray());
            }

            String alias = obtenerAlias(ks);
            PrivateKey clavePrivada = (PrivateKey) ks.getKey(alias, contrasenaCA.toCharArray());
            Certificate[] cadena = ks.getCertificateChain(alias);

            // Verificar vigencia del certificado CA
            ((X509Certificate) cadena[0]).checkValidity();

            // Firmar el PDF con iText 7 PdfSigner
            ByteArrayOutputStream baosFirmado = new ByteArrayOutputStream();
            try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfSinFirmar))) {
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

            System.out.println("[FirmadorPDFAdapter] PDF firmado con el certificado institucional de Key Vault. Alias: " + alias);
            return baosFirmado.toByteArray();

        } catch (FirmaPDFException e) {
            throw e;
        } catch (Exception e) {
            throw new FirmaPDFException("Error al firmar el PDF: " + e.getMessage(), e);
        }
    }

    private String obtenerAlias(KeyStore ks) throws KeyStoreException {
        Enumeration<String> aliases = ks.aliases();
        if (!aliases.hasMoreElements()) {
            throw new FirmaPDFException("El certificado institucional no contiene ningún alias.");
        }
        return aliases.nextElement();
    }
}
