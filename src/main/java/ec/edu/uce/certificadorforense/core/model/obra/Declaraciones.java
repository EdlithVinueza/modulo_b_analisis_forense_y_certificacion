package ec.edu.uce.certificadorforense.core.model.obra;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Declaraciones {
    private final boolean titularDerechos;
    private final boolean aceptaTerminos;

    public boolean isCompletas() {
        return titularDerechos && aceptaTerminos;
    }
}
