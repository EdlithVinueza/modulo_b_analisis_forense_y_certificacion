package ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public class ReglaFirmaEstructural implements IReglaValidacion<ArchivoImagen> {
    @Override
    public ResultadoValidacion validar(ArchivoImagen img) {
        String formatoBinario = img.getEstructura().getFormatoReal();
        String extension = img.getNombreArchivo().toLowerCase();

        boolean esImagenValida = "PNG".equals(formatoBinario) || "JPEG".equals(formatoBinario);
        boolean coincideConExtension = extension.endsWith(formatoBinario.toLowerCase()) ||
                (formatoBinario.equals("JPEG") && (extension.endsWith("jpg") || extension.endsWith("jpeg")));

        boolean pasa = esImagenValida && coincideConExtension;

        return ResultadoValidacion.builder()
                .nombreRegla("Integridad de Firma Binaria")
                .esValido(pasa)
                .mensaje(pasa ? "Estructura válida: " + formatoBinario :
                        "FRAUDE: Firma binaria (" + formatoBinario + ") no coincide con extensión.")
                .build();
    }

    @Override public boolean esCritica() { return true; }
}
