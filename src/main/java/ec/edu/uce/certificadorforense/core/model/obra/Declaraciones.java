package ec.edu.uce.certificadorforense.core.model.obra;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Declaraciones obligatorias que el autor debe aceptar antes de avanzar a Fase 3.
 * Las tres deben ser {@code true} para que el proceso pueda continuar.
 */
@Getter
@Builder
@ToString
public class Declaraciones {

    /** El autor declara ser titular o poseer derechos sobre la obra. */
    private final boolean titularDerechos;

    /** El autor comprende que el sistema realiza una certificación técnica, no legal. */
    private final boolean entiendeCertificacionTecnica;

    /** El autor acepta los términos de uso del sistema Verisart. */
    private final boolean aceptaTerminos;

    /**
     * Verifica que todas las declaraciones hayan sido aceptadas.
     *
     * @return {@code true} si las tres declaraciones son {@code true}.
     */
    public boolean isCompletas() {
        return titularDerechos && entiendeCertificacionTecnica && aceptaTerminos;
    }
}
