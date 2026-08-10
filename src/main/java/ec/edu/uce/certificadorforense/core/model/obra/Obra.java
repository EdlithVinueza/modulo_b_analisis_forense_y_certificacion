package ec.edu.uce.certificadorforense.core.model.obra;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Builder
@ToString
public class Obra {
    private final String titulo;
    private final String descripcion;
    private final String software;
    private final String hardware;
    private final CategoriaObra categoria;
    private final LocalDate fechaCreacion;
}
