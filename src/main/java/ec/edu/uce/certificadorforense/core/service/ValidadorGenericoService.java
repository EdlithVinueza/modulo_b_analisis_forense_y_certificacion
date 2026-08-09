package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import ec.edu.uce.certificadorforense.core.rules.IReglaValidacion;

import java.util.ArrayList;
import java.util.List;

public class ValidadorGenericoService<T extends ArchivoBase> {
    private final List<IReglaValidacion<T>> reglas = new ArrayList<>();

    public void registrarRegla(IReglaValidacion<T> regla) {
        this.reglas.add(regla);
    }

    public VeredictoFinal validar(T objeto) {
        VeredictoFinal veredicto = new VeredictoFinal();

        for (IReglaValidacion<T> regla : reglas) {
            // 1. Ejecutar la validación individual
            ResultadoValidacion resultado = regla.validar(objeto);

            // 2. Agregar al veredicto
            veredicto.agregarResultado(resultado, regla.esCritica());

            // 3. Cortocircuito si tu veredicto marca rechazo
            if (veredicto.isEsRechazado()) {
                break;
            }
        }

        return veredicto;
    }
}
