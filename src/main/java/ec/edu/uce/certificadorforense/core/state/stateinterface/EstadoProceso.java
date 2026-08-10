package ec.edu.uce.certificadorforense.core.state.stateinterface;

import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

public interface EstadoProceso {
    void ejecutar(ContextoProceso contexto);
    boolean validar(ContextoProceso contexto);
    void avanzar(ContextoProceso contexto);
    void retroceder(ContextoProceso contexto);
    String getNombre();
}
