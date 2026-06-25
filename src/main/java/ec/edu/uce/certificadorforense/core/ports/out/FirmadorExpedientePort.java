package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;

import java.io.File;

/**
 * Puerto de salida — Firma digital del expediente con el P12 del autor.
 */
public interface FirmadorExpedientePort {

    /**
     * Firma el JSON del expediente con la clave privada del P12 del autor.
     *
     * @param expedienteJson JSON serializado del expediente.
     * @param archivoPkcs12  Archivo {@code .p12} del autor.
     * @param contrasena     Contraseña del keystore.
     * @return {@link FirmaAutor} con la firma en Base64 y metadatos.
     * @throws FirmaException si el P12 es inválido, la contraseña es incorrecta
     *                        o el certificado está expirado.
     */
    FirmaAutor firmar(String expedienteJson, File archivoPkcs12, String contrasena);

    /**
     * Valida el archivo P12 sin firmar: existencia, contraseña y vigencia.
     *
     * @param archivoPkcs12 Archivo {@code .p12} a validar.
     * @param contrasena    Contraseña del keystore.
     * @throws FirmaException si alguna validación falla.
     */
    void validar(File archivoPkcs12, String contrasena);

    /**
     * Excepción específica para errores de firma del expediente.
     */
    class FirmaException extends RuntimeException {
        public FirmaException(String mensaje) {
            super(mensaje);
        }
        public FirmaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
