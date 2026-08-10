package ec.edu.uce.certificadorforense.core.state.stateimplement;

import ec.edu.uce.certificadorforense.core.state.stateinterface.EstadoProceso;

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
