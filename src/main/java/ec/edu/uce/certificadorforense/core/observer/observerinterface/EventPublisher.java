package ec.edu.uce.certificadorforense.core.observer.observerinterface;

public interface EventPublisher {
    void suscribir(EventListener listener);
    void desuscribir(EventListener listener);
    void publicar(EventoSistema evento);
}
