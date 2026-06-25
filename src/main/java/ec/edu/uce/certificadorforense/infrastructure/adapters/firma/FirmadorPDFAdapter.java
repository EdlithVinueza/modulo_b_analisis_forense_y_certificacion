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
 * Adaptador de infraestructura: firma digital del PDF con el certificado institucional {@code root_ca.p12}.
 * <p>
 * Usa iText 7 {@code PdfSigner} con el proveedor criptográfico BouncyCastle.
 * La ruta del certificado CA es fija: {@code /home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/sistema_certificado/sistema.p12}.
 * </p>
 */
public class FirmadorPDFAdapter implements FirmadorPDFPort {

    private final String rutaRootCa;

    public FirmadorPDFAdapter(String rutaRootCa) {
        this.rutaRootCa = rutaRootCa;
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
            File archivoCa = new File(this.rutaRootCa);
            if (!archivoCa.exists()) {
                throw new FirmaPDFException("No se encontró root_ca.p12 en: " + archivoCa.getAbsolutePath());
            }

            // Cargar el keystore del CA institucional
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(archivoCa)) {
                ks.load(fis, contrasenaCA.toCharArray());
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

            System.out.println("[FirmadorPDFAdapter] PDF firmado con root_ca.p12. Alias: " + alias);
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
            throw new FirmaPDFException("El root_ca.p12 no contiene ningún alias.");
        }
        return aliases.nextElement();
    }
}
