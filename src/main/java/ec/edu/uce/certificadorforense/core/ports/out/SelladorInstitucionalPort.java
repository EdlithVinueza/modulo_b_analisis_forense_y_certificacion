package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida: Sellado institucional del PDF con certificado de la CA.
 * <p>
 * Abstracción hexagonal que desacopla el Core del mecanismo concreto
 * de sellado (Azure Key Vault, HSM local, etc.).
 * </p>
 */
public interface SelladorInstitucionalPort {

    /**
     * Aplica el sello digital institucional sobre el PDF generado.
     *
     * @param pdfBytes Bytes del PDF sin sellar.
     * @param password Contraseña del keystore institucional.
     * @return Bytes del PDF con el sello institucional aplicado.
     * @throws SelloException si el proceso de sellado falla.
     */
    byte[] sellar(byte[] pdfBytes, String password);

    /** Excepción de dominio para fallos en el sellado institucional. */
    class SelloException extends RuntimeException {
        public SelloException(String message, Throwable cause) {
            super(message, cause);
        }
        public SelloException(String message) {
            super(message);
        }
    }
}
