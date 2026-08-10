package ec.edu.uce.certificadorforense.core.model.psd;

import lombok.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadatosPSD {

    private int altoImagen;
    private int anchoImagen;

    private String infoResolucion;
    private String datosMiniatura;

    private String nombreArchivoDetectado;
    private String software;

    private static final Pattern DPI_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    public double getDpiHorizontal() {
        return parsePrimerDpi(infoResolucion);
    }

    public double getDpiVertical() {
        return parseSegundoDpi(infoResolucion);
    }

    public boolean tieneResolucionProfesional() {
        return getDpiHorizontal() >= 150.0 && getDpiVertical() >= 150.0;
    }

    public boolean isTieneThumbnail() {
        return datosMiniatura != null && !datosMiniatura.isBlank();
    }

    private static double parsePrimerDpi(String texto) {
        double[] valores = extraerDpis(texto);
        return valores[0];
    }

    private static double parseSegundoDpi(String texto) {
        double[] valores = extraerDpis(texto);
        return valores[1] > 0 ? valores[1] : valores[0];
    }

    private static double[] extraerDpis(String texto) {
        double[] valores = new double[] {0d, 0d};
        if (texto == null || texto.isBlank()) return valores;
        Matcher matcher = DPI_PATTERN.matcher(texto.replace(',', '.'));
        int indice = 0;
        while (matcher.find() && indice < valores.length) {
            try {
                valores[indice++] = Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return valores;
    }
}
