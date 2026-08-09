package ec.edu.uce.certificadorforense.core.observerinterface;

/**
 * Interfaz del observador en el patrón Observer.
 */
public interface EventListener {

    void onEvento(EventoSistema evento);

    boolean soporta(EventoSistema evento);
}
