package ec.edu.uce.certificadorforense.core.state.stateimplement;

import ec.edu.uce.certificadorforense.core.state.stateinterface.EstadoProceso;

public class CertificacionState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        return contexto.getPdfCertificado() != null
                && contexto.getPdfCertificado().length > 0
                && contexto.getImagenCertificada() != null
                && contexto.getCertificado() != null;
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        throw new IllegalStateException(
            "[" + getNombre() + "] Este es el estado terminal. No hay estado siguiente."
        );
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        contexto.setEstadoActual(new FirmaAutorState());
        System.out.println("[Estado] Retrocediendo a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public String getNombre() {
        return "CERTIFICACION";
    }
}
