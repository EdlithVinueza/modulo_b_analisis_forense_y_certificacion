package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida: Firma digital en la nube (Azure / CA externa).
 * <p>
 * Abstracción hexagonal que desacopla el Core de la implementación
 * concreta de Azure Key Vault o cualquier proveedor de firma digital.
 * </p>
 */
public interface FirmadorNubePort {

    /**
     * Envía el hash de la obra a la CA externa y obtiene la firma digital.
     *
     * @param p12Base64 Credencial P12 del autor en Base64.
     * @param password  Contraseña del keystore P12.
     * @param hashObra  Hash SHA-512 del expediente serializado.
     * @return Firma digital en Base64.
     * @throws FirmaNubeException si la CA rechaza la solicitud o hay error de conexión.
     */
    String firmar(String p12Base64, String password, String hashObra);

    /** Excepción de dominio para fallos de firma en la nube. */
    class FirmaNubeException extends RuntimeException {
        public FirmaNubeException(String message, Throwable cause) {
            super(message, cause);
        }
        public FirmaNubeException(String message) {
            super(message);
        }
    }
}
