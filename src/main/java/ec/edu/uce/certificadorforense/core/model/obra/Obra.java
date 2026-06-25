package ec.edu.uce.certificadorforense.core.model.obra;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Representa la obra digital que se somete al proceso de certificación.
 */
@Getter
@Builder
@ToString
public class Obra {

    /** Título de la obra. */
    private final String titulo;

    /** Descripción de la obra. */
    private final String descripcion;

    /** Software utilizado para crear la obra (ej. Adobe Photoshop, Procreate). */
    private final String software;

    /** Hardware utilizado para crear la obra (ej. Tableta gráfica Wacom). */
    private final String hardware;

    /** Categoría de la obra. */
    private final CategoriaObra categoria;

    /** Fecha declarada de creación por el autor. */
    private final LocalDate fechaCreacion;
}
