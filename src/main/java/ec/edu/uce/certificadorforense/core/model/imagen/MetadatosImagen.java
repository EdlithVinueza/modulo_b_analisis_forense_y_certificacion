package ec.edu.uce.certificadorforense.core.model.imagen;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadatosImagen {
    // [1. Estructura de Píxeles]
    private int ancho;
    private int alto;
    private String tipoColor; // "True Color with Alpha" delata capturas

    // [2. Resolución Física - Huella de Origen]
    private long pixelesPorUnidadX;
    private long pixelesPorUnidadY;
    private String unidadFisica;

    // [3. Espacio de Color y Corrección - Huella de Procesamiento]
    private String intentoInterpretacion; // sRGB
    private String valorGamma;           // gAMA (0.45455 es típico de web)
    private boolean tienePerfilIcc;
    private String descripcionPerfilIcc;

    // [4. Identidad del Archivo]
    private String tipoMime;
    private String extensionReal;

    public double getDpiCalculado() {
        if (pixelesPorUnidadX <= 0) return 72.0; // Si no hay dato, es resolución web estándar
        if ("Metres".equalsIgnoreCase(unidadFisica)) {
            return Math.round((pixelesPorUnidadX / 100.0) * 2.54);
        }
        return (double) pixelesPorUnidadX;
    }

    @Override
    public String toString() {
        return "\n======= REPORTE TÉCNICO DE IMAGEN =======" +
                "\n• Dimensiones:       " + ancho + " x " + alto + " px" +
                "\n• Tipo de Color:     " + (tipoColor != null ? tipoColor : "N/A") +
                "\n• DPI (X/Y):         " + String.format("%.1f", getDpiCalculado()) + " (" + pixelesPorUnidadX + " / " + pixelesPorUnidadY + " " + unidadFisica + ")" +
                "\n• Tiene Perfil ICC: " +   tienePerfilIcc +
                "\n• Descripcion Perfil ICC:        " + tienePerfilIcc +
                "\n• Bloque sRGB:       " + intentoInterpretacion  +
                "\n• Bloque Gamma:      " + valorGamma  +
                "\n• MIME:        " + tipoMime   +
                "\n• Ext:        " + extensionReal +
                "\n==========================================";
    }
}
