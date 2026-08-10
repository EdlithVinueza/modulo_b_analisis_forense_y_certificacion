package ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public class ReglaAnalisisOrigen implements IReglaValidacion<ArchivoImagen> {
    @Override
    public ResultadoValidacion validar(ArchivoImagen img) {
        double dpi = img.getMetadatos().getDpiCalculado();
        String gamma = img.getMetadatos().getValorGamma();
        String srgb = img.getMetadatos().getIntentoInterpretacion();

        if (dpi >= 90.0 && dpi <= 125.0) {
            if (gamma != null || srgb != null) {
                return ResultadoValidacion.builder()
                        .nombreRegla("Análisis Forense de Origen")
                        .esValido(false)
                        .mensaje("VEREDICTO: RECORTE DE PANTALLA. Resolución de monitor y perfiles de sistema detectados.")
                        .build();
            }
        }

        boolean sinSoftware = (img.getMetadatos().getSoftware() == null || img.getMetadatos().getSoftware().isBlank());
        
        if (dpi <= 96.0 && (sinSoftware || !img.getMetadatos().isTienePerfilIcc())) {
            return ResultadoValidacion.builder()
                    .nombreRegla("Análisis Forense de Origen")
                    .esValido(false)
                    .mensaje("VEREDICTO: IMAGEN DE INTERNET. Resolución baja/web y sin metadatos de autoría original (Software o Perfil ICC ausente).")
                    .build();
        }

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
