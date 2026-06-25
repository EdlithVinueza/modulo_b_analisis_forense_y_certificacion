package ec.edu.uce.certificadorforense.core.model.validacion;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ResultadoValidacion {
    private final String nombreRegla;
    private final boolean esValido;
    private final String mensaje;
}
