package ec.edu.uce.certificadorforense.core.state;

/**
 * Estado 4 de 4 — Emisión de Certificado (Fase 4). Estado terminal.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Generar el ID del certificado ({@code CERT-YYYY-NNNNNN}).</li>
 *   <li>Generar el QR con el ID interno.</li>
 *   <li>Generar el PDF visual del certificado (iText 7).</li>
 *   <li>Firmar el PDF con {@code root_ca.p12} institucional.</li>
 *   <li>Inyectar el JSON de certificación en la imagen PNG (chunk {@code tEXt}).</li>
 *   <li>Publicar {@code EventoCertificadoEmitido}.</li>
 * </ul>
 * </p>
 * <p>No tiene estado siguiente — es el estado terminal del proceso.</p>
 */
public class CertificacionState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        // El certificado es válido cuando el PDF y la imagen certificada existen
        return contexto.getPdfCertificado() != null
                && contexto.getPdfCertificado().length > 0
                && contexto.getImagenCertificada() != null
                && contexto.getCertificado() != null;
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        throw new IllegalStateException(
            "[" + getNombre() + "] Este es el estado terminal. No hay estado siguiente."
        );
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        // Se puede retroceder a Firma para re-emitir, pero solo en casos de error
        contexto.setEstadoActual(new FirmaAutorState());
        System.out.println("[Estado] Retrocediendo a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public String getNombre() {
        return "CERTIFICACION";
    }
}
