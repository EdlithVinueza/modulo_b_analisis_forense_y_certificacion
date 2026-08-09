package ec.edu.uce.certificadorforense.core.model.imagen;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadatosImagen {
    // Persistidos en Base de Datos (evidencia_tecnica_json)
    private int ancho;
    private int alto;
    private String extensionReal;

    // Evaluados en Reglas de Análisis Forense
    private String tipoColor;
    private long pixelesPorUnidadX;
    private long pixelesPorUnidadY;
    private String unidadFisica;
    private String intentoInterpretacion;
    private String valorGamma;
    private boolean tienePerfilIcc;
    private String descripcionPerfilIcc;
    private String software;

    public double getDpiCalculado() {
        if (pixelesPorUnidadX <= 0) return 72.0;
        if ("Metres".equalsIgnoreCase(unidadFisica)) {
            return Math.round((pixelesPorUnidadX / 100.0) * 2.54);
        }
        return (double) pixelesPorUnidadX;
    }
}

