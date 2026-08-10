package ec.edu.uce.certificadorforense.core.state.stateimplement;

import ec.edu.uce.certificadorforense.core.state.stateinterface.EstadoProceso;

public class FirmaAutorState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
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
