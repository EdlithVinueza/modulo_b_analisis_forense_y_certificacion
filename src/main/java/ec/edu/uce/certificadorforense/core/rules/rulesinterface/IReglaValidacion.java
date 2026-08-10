package ec.edu.uce.certificadorforense.core.rules.rulesinterface;

import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public interface IReglaValidacion<T> {
    ResultadoValidacion validar(T objeto);
    boolean esCritica();
}
