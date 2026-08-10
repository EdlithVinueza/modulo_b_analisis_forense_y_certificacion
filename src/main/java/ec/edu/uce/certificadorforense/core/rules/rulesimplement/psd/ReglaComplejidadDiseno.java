package ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

public class ReglaComplejidadDiseno implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        int totalCapas = psd.getCapas().size();

        long conTrabajo = psd.getCapas().stream().filter(c ->
                c.isTieneMascaraCapa() || c.isTieneEfectos() || c.isEsClippingMask() ||
                        c.getTipo() == EstructuraCapaPSD.Tipo.TEXTO || !"norm".equals(c.getBlendModeKey()) ||
                        c.getOpacidadRaw() < 255 ||
                        (c.getAncho() > 0 && c.getAlto() > 0 && 
                        (c.getAncho() < psd.getMetadatos().getAnchoImagen() || c.getAlto() < psd.getMetadatos().getAltoImagen()))
        ).count();

        boolean esValido;
        String mensaje;

        if (totalCapas <= 3) {
            esValido = conTrabajo >= 1;
            mensaje = "Pocas capas (" + totalCapas + "). Requiere al menos 1 capa técnica. Encontradas: " + conTrabajo;
        } else {
            esValido = conTrabajo >= 2;
            mensaje = "Estructura compleja (" + totalCapas + " capas). Requiere al menos 2 capas técnicas. Encontradas: " + conTrabajo;
        }

        return ResultadoValidacion.builder()
                .nombreRegla("Autenticidad Técnica")
                .esValido(esValido)
                .mensaje(esValido ? "OK" : mensaje)
                .build();
    }

    @Override public boolean esCritica() { return true; }
}
