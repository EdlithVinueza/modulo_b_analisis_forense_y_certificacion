package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AnalisisResumen {
    private final String resultado;
    private final int capasPSD;
    private final boolean metadatosDetectados;
    private final String dimensiones;
    private final String detallesTecnicos;
}
