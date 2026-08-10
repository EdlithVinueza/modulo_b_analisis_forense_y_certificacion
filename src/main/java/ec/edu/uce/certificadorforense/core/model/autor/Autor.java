package ec.edu.uce.certificadorforense.core.model.autor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Autor {
    private String nombres;
    private String apellidos;
    private String cedula;
    private String correo;
    private String seudonimo;

    /** Nombre completo concatenado para display. */
    public String getNombreCompleto() {
        String n = nombres != null ? nombres : "";
        String a = apellidos != null ? apellidos : "";
        return (n + " " + a).trim();
    }
}
