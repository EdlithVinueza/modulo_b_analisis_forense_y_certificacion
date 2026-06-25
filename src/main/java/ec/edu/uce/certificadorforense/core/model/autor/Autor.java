package ec.edu.uce.certificadorforense.core.model.autor;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Representa al autor de la obra digital que solicita la certificación.
 */
@Getter
@Builder
@ToString
public class Autor {

    /** Nombres del autor. */
    private final String nombres;

    /** Apellidos del autor. */
    private final String apellidos;

    /** Número de cédula de identidad. */
    private final String cedula;

    /** Correo electrónico del autor. */
    private final String correo;

    /** Seudónimo artístico (opcional, puede ser null o vacío). */
    private final String seudonimo;

    /**
     * Devuelve el nombre completo (nombres + apellidos).
     */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    /**
     * Indica si el autor tiene seudónimo registrado.
     */
    public boolean tieneSeudonimo() {
        return seudonimo != null && !seudonimo.isBlank();
    }
}
