package ec.edu.uce.certificadorforense.core.state;

/**
 * Estado 1 de 4 — Análisis Forense (Fase 1).
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Publicar {@code EventoAnalisisIniciado} → dispara HashGenerator, MetadataExtractor, CapasExtractor.</li>
 *   <li>Ejecutar las reglas de validación PSD e imagen.</li>
 *   <li>Calcular el pHash comparativo.</li>
 *   <li>Si el resultado es APROBADO, avanzar a {@link DatosObraState}.</li>
 * </ul>
 * </p>
 */
public class AnalisisForenseState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        // La lógica de este estado será orquestada por el servicio correspondiente.
        // La implementación concreta se delega al ProcesoCertificacionRunner
        // y los listeners del Observer.
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        // El análisis es válido si ambos veredictos existen y ninguno está rechazado
        return contexto.getVeredictoPSD() != null
                && contexto.getVeredictoImagen() != null
                && !contexto.getVeredictoPSD().isEsRechazado()
                && !contexto.getVeredictoImagen().isEsRechazado()
                && contexto.getPHash() != null
                && contexto.getSha512PSD() != null
                && contexto.getSha512Imagen() != null;
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        if (!validar(contexto)) {
            throw new IllegalStateException(
                "[" + getNombre() + "] No se puede avanzar: el análisis forense no fue APROBADO."
            );
        }
        contexto.setEstadoActual(new DatosObraState());
        System.out.println("[Estado] Avanzando a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        throw new IllegalStateException(
            "[" + getNombre() + "] Este es el primer estado. No hay estado anterior."
        );
    }

    @Override
    public String getNombre() {
        return "ANALISIS_FORENSE";
    }
}
