package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import ec.edu.uce.certificadorforense.core.ports.out.SelladorInstitucionalPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorPDFAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;

/**
 * Adaptador de Infraestructura para el sellado institucional de PDFs.
 * Implementa {@link SelladorInstitucionalPort} utilizando {@link FirmadorPDFAdapter}
 * y el certificado PKCS#12 institucional.
 */
@ApplicationScoped
public class VaultSealAdapter implements SelladorInstitucionalPort {

    @ConfigProperty(name = "tesis.cert.path", defaultValue = "C:/Users/edlit/OneDrive/Documentos/TESIS/Archivos de Prueba/firma .p12/firma_9900000003.p12")
    String rutaCertificado;

    @Override
    public byte[] sellar(byte[] pdfBytes, String password) {
        try {
            File fileCa = new File(rutaCertificado);
            if (!fileCa.exists()) {
                System.err.println("[VaultSealAdapter] Advertencia: No se encontró el certificado en " 
                        + fileCa.getAbsolutePath() + ". Se retorna PDF sin sellar.");
                return pdfBytes;
            }

            FirmadorPDFAdapter firmador = new FirmadorPDFAdapter(rutaCertificado);
            return firmador.firmarPDF(pdfBytes, password);
        } catch (Exception e) {
            System.err.println("[VaultSealAdapter] Error durante el sellado institucional: " + e.getMessage());
            return pdfBytes;
        }
    }
}
