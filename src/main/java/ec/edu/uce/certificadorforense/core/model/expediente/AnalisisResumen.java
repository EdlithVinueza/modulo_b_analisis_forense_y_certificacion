package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Resumen de los resultados del análisis forense (Fase 1).
 * Forma parte del expediente JSON que se firma en Fase 3.
 */
@Getter
@Builder
@ToString
public class AnalisisResumen {

    /**
     * Resultado del análisis forense.
     * Valores posibles: {@code "APROBADO"} o {@code "RECHAZADO"}.
     */
    private final String resultado;

    /** Número de capas binarias detectadas en el archivo PSD. */
    private final int capasPSD;

    /** Indica si se detectaron metadatos en el archivo PSD. */
    private final boolean metadatosDetectados;

    /** Dimensiones de la obra en píxeles. */
    private final String dimensiones;

    /** Detalles técnicos generados en fase 1. */
    private final String detallesTecnicos;
}
