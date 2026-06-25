package ec.edu.uce.certificadorforense.core.ports.in;

import java.io.File;

/**
 * Puerto de entrada — Caso de uso: Firmar Expediente (Fase 3).
 * <p>
 * El autor proporciona su archivo P12 y contraseña. El sistema construye el
 * expediente JSON, valida el certificado y aplica la firma digital.
 * </p>
 */
public interface FirmarExpedienteUseCase {

    /**
     * Construye y firma el expediente con el certificado del autor.
     *
     * @param archivoPkcs12 Archivo {@code .p12} del autor.
     * @param contrasena    Contraseña del keystore P12.
     */
    void ejecutar(File archivoPkcs12, String contrasena);
}
