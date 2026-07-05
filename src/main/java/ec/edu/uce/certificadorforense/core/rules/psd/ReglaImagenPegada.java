package ec.edu.uce.certificadorforense.core.rules.psd;

import ec.edu.uce.certificadorforense.core.rules.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

public class ReglaImagenPegada implements IReglaValidacion<ArchivoPSD> {
    public ResultadoValidacion validar(ArchivoPSD psd) {
        int lienzoW = psd.getMetadatos().getAnchoImagen();
        int lienzoH = psd.getMetadatos().getAltoImagen();

        boolean todasSonPlanas = true;
        for (EstructuraCapaPSD capa : psd.getCapas()) {
            boolean esCapaPlana = (capa.getAncho() == lienzoW && capa.getAlto() == lienzoH) &&
                    !capa.isTieneMascaraCapa() && !capa.isTieneEfectos() &&
                    !capa.isEsClippingMask() && "norm".equals(capa.getBlendModeKey());
            
            if (!esCapaPlana) {
                todasSonPlanas = false;
                break;
            }
        }

        if (todasSonPlanas && !psd.getCapas().isEmpty()) {
            return ResultadoValidacion.builder()
                    .nombreRegla("Fraude: Imagen Pegada")
                    .esValido(false)
                    .mensaje("Se detectó que todas las capas cubren todo el lienzo sin edición técnica (posible copia/pega).")
                    .build();
        }

        return ResultadoValidacion.builder()
                .nombreRegla("Fraude: Imagen Pegada")
                .esValido(true)
                .mensaje("OK")
                .build();
    }

    @Override public boolean esCritica() { return true; }
}
