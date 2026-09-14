package ec.edu.uce.certificadorforense.core.ports.in;

/**
 * Puerto de entrada — Caso de uso: Emitir Certificado y Generar Paquete ZIP (Fase 4).
 */
public interface EmitirCertificadoUseCase {
    byte[] emitir(String idExpediente) throws Exception;
    byte[] obtenerZipYLimpiar(String idExpediente);
    boolean esPropietario(String idExpediente, String cedulaSolicitante);
}
