package ec.edu.uce.certificadorforense.core.observer.listeners;

import ec.edu.uce.certificadorforense.core.observer.EventListener;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoFirmaRealizada;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;

/**
 * Listener: al {@link EventoFirmaRealizada}, persiste el expediente
 * y la firma del autor mediante {@link ExpedienteRepositoryPort}.
 */
public class ExpedienteListener implements EventListener {

    private final ExpedienteRepositoryPort repositorio;

    public ExpedienteListener(ExpedienteRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return evento instanceof EventoFirmaRealizada;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        EventoFirmaRealizada e = (EventoFirmaRealizada) evento;
        System.out.println("[ExpedienteListener] Persistiendo expediente: "
                + e.getExpediente().getIdExpediente());

        repositorio.guardar(
                e.getExpediente(),
                e.getExpedienteJson(),
                e.getFirmaAutor().getFirmaBase64()
        );

        System.out.println("  Expediente guardado exitosamente.");
    }
}
