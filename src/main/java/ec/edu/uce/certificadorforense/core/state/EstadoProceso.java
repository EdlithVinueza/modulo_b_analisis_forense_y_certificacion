package ec.edu.uce.certificadorforense.core.state;

/**
 * Interfaz central del patrón State para el flujo de certificación Verisart.
 * <p>
 * Cada fase del proceso es un estado concreto que sabe:
 * <ul>
 *   <li>qué puede hacer ({@link #ejecutar});</li>
 *   <li>cuál es el siguiente ({@link #avanzar});</li>
 *   <li>cuál es el anterior ({@link #retroceder});</li>
 *   <li>si sus datos son válidos ({@link #validar}).</li>
 * </ul>
 * </p>
 *
 * <p>Flujo de estados:</p>
 * <pre>
 * AnalisisForenseState → DatosObraState → FirmaAutorState → CertificacionState
 * </pre>
 */
public interface EstadoProceso {

    /**
     * Ejecuta la lógica principal de este estado.
     *
     * @param contexto Contexto compartido del proceso.
     */
    void ejecutar(ContextoProceso contexto);

    /**
     * Valida si el estado actual tiene datos suficientes para avanzar.
     *
     * @param contexto Contexto compartido del proceso.
     * @return {@code true} si se puede avanzar al siguiente estado.
     */
    boolean validar(ContextoProceso contexto);

    /**
     * Avanza al siguiente estado si la validación es exitosa.
     *
     * @param contexto Contexto compartido del proceso.
     * @throws IllegalStateException si {@link #validar} retorna {@code false}.
     */
    void avanzar(ContextoProceso contexto);

    /**
     * Retrocede al estado anterior.
     *
     * @param contexto Contexto compartido del proceso.
     * @throws IllegalStateException si este es el primer estado (no hay anterior).
     */
    void retroceder(ContextoProceso contexto);

    /**
     * Nombre identificador del estado (para logging y debugging).
     *
     * @return Nombre del estado.
     */
    String getNombre();
}
