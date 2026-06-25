package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida — Firma digital del PDF con el certificado institucional {@code root_ca.p12}.
 */
public interface FirmadorPDFPort {

    /**
     * Firma digitalmente un PDF con la clave privada del CA institucional.
     * <p>
     * Internamente carga {@code documentos/certificados/root_ca.p12},
     * extrae la clave privada y aplica {@code PdfSigner.signDetached()} de iText 7.
     * </p>
     *
     * @param pdfSinFirmar  Bytes del PDF generado sin firma.
     * @param contrasenaCA  Contraseña del keystore {@code root_ca.p12}.
     * @return Bytes del PDF con la firma digital CMS/CAdES incrustada.
     * @throws FirmaPDFException si el CA no puede cargarse o la firma falla.
     */
    byte[] firmarPDF(byte[] pdfSinFirmar, String contrasenaCA);

    /**
     * Excepción específica para errores de firma del PDF.
     */
    class FirmaPDFException extends RuntimeException {
        public FirmaPDFException(String mensaje) {
            super(mensaje);
        }
        public FirmaPDFException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
