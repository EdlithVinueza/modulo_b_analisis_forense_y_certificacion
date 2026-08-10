package ec.edu.uce.certificadorforense.core.state.stateimplement;

import ec.edu.uce.certificadorforense.core.state.stateinterface.EstadoProceso;

public class AnalisisForenseState implements EstadoProceso {

    @Override
    public void ejecutar(ContextoProceso contexto) {
        System.out.println("[Estado] Ejecutando: " + getNombre());
    }

    @Override
    public boolean validar(ContextoProceso contexto) {
        return contexto.getVeredictoPSD() != null
                && contexto.getVeredictoImagen() != null
                && !contexto.getVeredictoPSD().isEsRechazado()
                && !contexto.getVeredictoImagen().isEsRechazado()
                && contexto.getPHash() != null
                && contexto.getSha512PSD() != null
                && contexto.getSha512Imagen() != null;
    }

    @Override
    public void avanzar(ContextoProceso contexto) {
        if (!validar(contexto)) {
            throw new IllegalStateException(
                "[" + getNombre() + "] No se puede avanzar: el análisis forense no fue APROBADO."
            );
        }
        contexto.setEstadoActual(new DatosObraState());
        System.out.println("[Estado] Avanzando a: " + contexto.getEstadoActual().getNombre());
    }

    @Override
    public void retroceder(ContextoProceso contexto) {
        throw new IllegalStateException(
            "[" + getNombre() + "] Este es el primer estado. No hay estado anterior."
        );
    }

    @Override
    public String getNombre() {
        return "ANALISIS_FORENSE";
    }
}
