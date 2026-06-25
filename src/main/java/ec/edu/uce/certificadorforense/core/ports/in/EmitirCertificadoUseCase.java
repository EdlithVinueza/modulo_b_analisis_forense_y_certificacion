package ec.edu.uce.certificadorforense.core.ports.in;

/**
 * Puerto de entrada — Caso de uso: Emitir Certificado (Fase 4).
 * <p>
 * Genera el certificado visual en PDF (firmado con {@code root_ca.p12})
 * y la imagen certificada (PNG con chunk {@code tEXt} inyectado).
 * Este caso de uso lo ejecuta la entidad, no el autor.
 * </p>
 */
public interface EmitirCertificadoUseCase {

    /**
     * Ejecuta la emisión del certificado completo.
     * Lee el expediente firmado del contexto y produce los archivos de salida.
     */
    void ejecutar();
}
