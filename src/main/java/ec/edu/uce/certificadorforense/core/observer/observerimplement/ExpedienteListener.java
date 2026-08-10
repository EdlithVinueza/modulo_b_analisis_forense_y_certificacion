package ec.edu.uce.certificadorforense.core.observer.observerimplement;

import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observer.observerinterface.EventoSistema;
import ec.edu.uce.certificadorforense.core.observer.eventos.EventoFirmaRealizada;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;

public class ExpedienteListener implements EventListener {

    private final ExpedienteRepositoryPort repositorio;

    public ExpedienteListener(ExpedienteRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (evento instanceof EventoFirmaRealizada ev) {
            System.out.println("[ExpedienteListener] Guardando expediente firmado ID: " + ev.getExpediente().getIdExpediente());
            String firmaBase64 = ev.getFirmaAutor() != null ? ev.getFirmaAutor().getFirmaBase64() : "";
            repositorio.guardar(ev.getExpediente(), ev.getExpedienteJson(), firmaBase64);
        }
    }
}
