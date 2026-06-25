package ec.edu.uce.certificadorforense.core.state;

/**
 * Estado 3 de 4 — Firma del Autor (Fase 3).
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Construir el {@code Expediente} JSON con datos de las fases anteriores.</li>
 *   <li>Validar el archivo P12 del autor.</li>
 *   <li>Firmar el expediente JSON con la clave privada del autor.</li>
 *   <li>Publicar {@code EventoFirmaRealizada} → dispara ExpedienteListener, AuditoriaListener, PDFGeneratorListener.</li>
 *   <li>Si la firma es exitosa, avanzar a {@link CertificacionState}.</li>
 * </ul>
 * </p>
 */
public class FirmaAutorState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        // La firma es válida cuando el expediente JSON y la firma del autor existen
        return contexto.getExpedienteJson() != null
                && !contexto.getExpedienteJson().isBlank()
                && contexto.getFirmaAutor() != null
                && contexto.getFirmaAutor().getFirmaBase64() != null;
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        if (!validar(contexto)) {
            throw new IllegalStateException(
                "[" + getNombre() + "] No se puede avanzar: el expediente no ha sido firmado."
            );
        }
        contexto.setEstadoActual(new CertificacionState());
        System.out.println("[Estado] Avanzando a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        contexto.setEstadoActual(new DatosObraState());
        System.out.println("[Estado] Retrocediendo a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public String getNombre() {
        return "FIRMA_AUTOR";
    }
}
