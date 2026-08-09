package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;
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
        repositorio.guardar(
                e.getExpediente(),
                e.getExpedienteJson(),
                e.getFirmaAutor().getFirmaBase64()
        );
    }
}
