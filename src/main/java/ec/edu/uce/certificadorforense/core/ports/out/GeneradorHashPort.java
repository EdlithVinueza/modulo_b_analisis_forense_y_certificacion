package ec.edu.uce.certificadorforense.core.ports.out;

/**
 * Puerto de salida — Generación de hash SHA-512.
 */
public interface GeneradorHashPort {

    /**
     * Calcula el hash SHA-512 de un arreglo de bytes.
     *
     * @param datos Bytes a hashear.
     * @return Hash en formato hexadecimal lowercase (128 caracteres).
     */
    String calcularSHA512(byte[] datos);

    /**
     * Calcula el hash SHA-512 de una cadena UTF-8.
     *
     * @param texto Texto a hashear.
     * @return Hash en formato hexadecimal lowercase (128 caracteres).
     */
    String calcularSHA512(String texto);
}
