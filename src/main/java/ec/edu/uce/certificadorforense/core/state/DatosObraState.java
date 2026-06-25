package ec.edu.uce.certificadorforense.core.state;

/**
 * Estado 2 de 4 — Datos de la Obra (Fase 2).
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Recibir y almacenar datos del {@code Autor}, {@code Obra} y {@code Declaraciones}.</li>
 *   <li>Verificar que las tres declaraciones estén aceptadas antes de avanzar.</li>
 *   <li>Si válido, avanzar a {@link FirmaAutorState}.</li>
 * </ul>
 * </p>
 */
public class DatosObraState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        return contexto.getAutor() != null
                && contexto.getObra() != null
                && contexto.getDeclaraciones() != null
                && contexto.getDeclaraciones().isCompletas();
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        if (!validar(contexto)) {
            throw new IllegalStateException(
                "[" + getNombre() + "] No se puede avanzar: faltan datos del autor, obra "
                + "o las declaraciones no están completas."
            );
        }
        contexto.setEstadoActual(new FirmaAutorState());
        System.out.println("[Estado] Avanzando a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        contexto.setEstadoActual(new AnalisisForenseState());
        System.out.println("[Estado] Retrocediendo a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public String getNombre() {
        return "DATOS_OBRA";
    }
}
