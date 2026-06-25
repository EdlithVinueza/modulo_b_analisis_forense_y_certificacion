package ec.edu.uce.certificadorforense.core.model.psd;

import lombok.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadatosPSD {

    // ========== CABECERA PSD ==========
    private int cantidadCanales;
    private int altoImagen;
    private int anchoImagen;
    private int bitsPorCanal;
    private String modoColor;

    // ========== DIRECTORIO PHOTOSHOP ==========
    private String infoResolucion;
    private String infoEstadoCapas;
    private String datosMiniatura;

    // ========== TIPO DE ARCHIVO ==========
    private String nombreArchivoDetectado;
    private String nombreLargoArchivoDetectado;
    private String tipoMimeDetectado;
    private String extensionEsperada;

    // ========== METADATOS EXTRA ==========
    private String software;
    private String autor;
    private String fechaCreacion;

    // ========== PERFIL ICC ==========
    private boolean tienePerfilIcc;
    private String descripcionPerfilIcc;
    private String clasePerfilIcc;
    private String espacioColorIcc;
    private String copyrightIcc;

    // --- Lógica para DPI (Estos se quedan como métodos porque CALCULAN datos) ---
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

    // --- Métodos de soporte privados ---
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====================================================");
        sb.append("\n   REPORTE TÉCNICO COMPLETO: ").append(nombreArchivoDetectado != null ? nombreArchivoDetectado : "Archivo PSD");
        sb.append("\n====================================================");

        // 1. CABECERA TÉCNICA
        sb.append("\n\n[1. CABECERA DEL ARCHIVO]");
        sb.append(String.format("\n  - Dimensiones       : %d x %d píxeles", anchoImagen, altoImagen));
        sb.append(String.format("\n  - Resolución        : %.2f x %.2f DPI (Original: %s)", getDpiHorizontal(), getDpiVertical(), infoResolucion));
        sb.append(String.format("\n  - Canales           : %d canales", cantidadCanales));
        sb.append(String.format("\n  - Profundidad       : %d bits por canal", bitsPorCanal));
        sb.append(String.format("\n  - Modo de Color     : %s", modoColor));

        // 2. DATOS DE PHOTOSHOP
        sb.append("\n\n[2. RECURSOS INTERNOS DE PHOTOSHOP]");
        sb.append(String.format("\n  - Estado de Capas   : %s", infoEstadoCapas != null ? infoEstadoCapas : "N/A"));
        sb.append(String.format("\n  - Miniatura (Thumb) : %s", isTieneThumbnail() ? "Presente" : "Ausente"));
        if (isTieneThumbnail()) {
            // Recortamos la cadena de la miniatura para no inundar la consola
            String thumbShort = datosMiniatura.length() > 60 ? datosMiniatura.substring(0, 60) + "..." : datosMiniatura;
            sb.append("\n    └─ Fragmento datos: ").append(thumbShort);
        }

        // 3. IDENTIFICACIÓN DE ARCHIVO
        sb.append("\n\n[3. IDENTIFICACIÓN Y SISTEMA]");
        sb.append(String.format("\n  - Nombre Largo      : %s", nombreLargoArchivoDetectado));
        sb.append(String.format("\n  - Tipo MIME         : %s", tipoMimeDetectado));
        sb.append(String.format("\n  - Extensión Sugerida: %s", extensionEsperada));
        sb.append(String.format("\n  - Software/Motor    : %s", software != null ? software : "Desconocido"));

        // 4. AUTORÍA Y TIEMPO
        sb.append("\n\n[4. METADATOS DE ORIGEN]");
        sb.append(String.format("\n  - Autor/Artista     : %s", autor != null ? autor : "No especificado"));
        sb.append(String.format("\n  - Fecha de Creación : %s", fechaCreacion != null ? fechaCreacion : "No disponible"));

        // 5. PERFIL DE COLOR (ICC)
        sb.append("\n\n[5. PERFIL DE COLOR ICC]");
        sb.append(String.format("\n  - ¿Tiene Perfil?    : %s", tienePerfilIcc ? "SÍ" : "NO"));
        if (tienePerfilIcc) {
            sb.append(String.format("\n  - Descripción       : %s", descripcionPerfilIcc));
            sb.append(String.format("\n  - Clase de Perfil   : %s", clasePerfilIcc));
            sb.append(String.format("\n  - Espacio de Color  : %s", espacioColorIcc));
            sb.append(String.format("\n  - Copyright ICC     : %s", copyrightIcc));
        }

        sb.append("\n\n[6. VALIDACIONES]");
        sb.append(String.format("\n  - ¿Calidad Pro?     : %s", tieneResolucionProfesional() ? "SÍ (>=150 DPI) ✅" : "NO ❌"));

        sb.append("\n====================================================\n");
        return sb.toString();
    }
}
