package ec.edu.uce.certificadorforense.core.model.validacion;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class VeredictoFinal {
    private final List<ResultadoValidacion> detalles = new ArrayList<>();
    private boolean esRechazado = false;
    private String razonRechazo;
    private int totalReglasEvaluadas = 0;
    private int reglasExitosas = 0;

    public void agregarResultado(ResultadoValidacion res, boolean critica) {
        this.detalles.add(res);
        this.totalReglasEvaluadas++;

        if (res.isEsValido()) {
            this.reglasExitosas++;
        } else if (critica) {
            this.esRechazado = true;
            this.razonRechazo = res.getNombreRegla();
        }
    }
}
