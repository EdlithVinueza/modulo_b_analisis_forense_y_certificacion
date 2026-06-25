package ec.edu.uce.certificadorforense.core.rules.psd;

import ec.edu.uce.certificadorforense.core.rules.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

public class ReglaImagenPegada implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        // SI TIENE MUCHAS CAPAS, NO PUEDE SER UN SIMPLE "COPY-PASTE" DE INTERNET
        // Ponemos un umbral de 5 capas. Si tiene más, esta regla se aprueba automáticamente.
        if (psd.getCapas().size() > 5) {
            return ResultadoValidacion.builder()
                    .nombreRegla("Fraude: Imagen Pegada")
                    .esValido(true)
                    .mensaje("Estructura compleja detectada (" + psd.getCapas().size() + " capas). No es una imagen plana.")
                    .build();
        }

        // SI TIENE POCAS CAPAS (<=5), BUSCAMOS SI ES UNA IMAGEN PLANA
        int lienzoW = psd.getMetadatos().getAnchoImagen();
        int lienzoH = psd.getMetadatos().getAltoImagen();

        for (EstructuraCapaPSD capa : psd.getCapas()) {
            if (capa.getAncho() == lienzoW && capa.getAlto() == lienzoH) {
                if (!capa.isTieneMascaraCapa() && !capa.isTieneEfectos() &&
                        !capa.isEsClippingMask() && "norm".equals(capa.getBlendModeKey())) {

                    return ResultadoValidacion.builder()
                            .nombreRegla("Fraude: Imagen Pegada")
                            .esValido(false)
                            .mensaje("Se detectó una capa única que cubre todo el lienzo sin edición técnica.")
                            .build();
                }
            }
        }

        return ResultadoValidacion.builder()
                .nombreRegla("Fraude: Imagen Pegada")
                .esValido(true)
                .mensaje("OK")
                .build();
    }

    @Override public boolean esCritica() { return true; }
}
