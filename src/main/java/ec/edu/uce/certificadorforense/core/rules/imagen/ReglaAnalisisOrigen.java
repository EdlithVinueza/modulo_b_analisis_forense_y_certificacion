package ec.edu.uce.certificadorforense.core.rules.imagen;

import ec.edu.uce.certificadorforense.core.rules.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

/**
 * Regla de validación que analiza la densidad de píxeles (DPI) y los perfiles de color (Gamma, sRGB)
 * para detectar recortes de pantalla, imágenes descargadas de internet y validarlas frente
 * a exportaciones de software profesional.
 */
public class ReglaAnalisisOrigen implements IReglaValidacion<ArchivoImagen> {
    @Override
    public ResultadoValidacion validar(ArchivoImagen img) {
        double dpi = img.getMetadatos().getDpiCalculado();
        String gamma = img.getMetadatos().getValorGamma();
        String srgb = img.getMetadatos().getIntentoInterpretacion();

        // 1. DETECCIÓN DE RECORTE DE PANTALLA (96-125 DPI + Gamma/sRGB)
        if (dpi >= 90.0 && dpi <= 125.0) {
            if (gamma != null || srgb != null) {
                return ResultadoValidacion.builder()
                        .nombreRegla("Análisis Forense de Origen")
                        .esValido(false)
                        .mensaje("VEREDICTO: RECORTE DE PANTALLA. Resolución de monitor y perfiles de sistema detectados.")
                        .build();
            }
        }

        // 2. DETECCIÓN DE IMAGEN DE INTERNET (Baja resolución y sin huella de software)
        // Redes sociales como Pinterest o Facebook borran la etiqueta 'Software' y bajan la resolución.
        boolean sinSoftware = (img.getMetadatos().getSoftware() == null || img.getMetadatos().getSoftware().isBlank());
        
        if (dpi <= 96.0 && (sinSoftware || !img.getMetadatos().isTienePerfilIcc())) {
            return ResultadoValidacion.builder()
                    .nombreRegla("Análisis Forense de Origen")
                    .esValido(false)
                    .mensaje("VEREDICTO: IMAGEN DE INTERNET. Resolución baja/web y sin metadatos de autoría original (Software o Perfil ICC ausente).")
                    .build();
        }

        // 3. DETECCIÓN DE ARCHIVO ORIGINAL / EXPORT
        if (dpi >= 300.0) {
            return ResultadoValidacion.builder()
                    .nombreRegla("Análisis Forense de Origen")
                    .esValido(true)
                    .mensaje("VEREDICTO: EXPORTACIÓN ORIGINAL. Alta densidad detectada (" + dpi + " DPI).")
                    .build();
        }

        return ResultadoValidacion.builder().nombreRegla("Análisis Forense de Origen").esValido(true).mensaje("Origen aceptable.").build();
    }
    @Override public boolean esCritica() { return true; }
}
