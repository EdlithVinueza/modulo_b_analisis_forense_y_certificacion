package ec.edu.uce.certificadorforense.core.observer;

/**
 * Interfaz del observador en el patrón Observer.
 * <p>
 * Cada listener concreto implementa esta interfaz y se suscribe a uno
 * o más tipos de {@link EventoSistema} a través del {@link EventPublisher}.
 * </p>
 */
public interface EventListener {

    /**
     * Método invocado cuando ocurre un evento al que este listener está suscrito.
     *
     * @param evento El evento ocurrido.
     */
    void onEvento(EventoSistema evento);

    /**
     * Indica si este listener puede manejar el tipo de evento dado.
     *
     * @param evento El evento a evaluar.
     * @return {@code true} si este listener maneja este tipo de evento.
     */
    boolean soporta(EventoSistema evento);
}
