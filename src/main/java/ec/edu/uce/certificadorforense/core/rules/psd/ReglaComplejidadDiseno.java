package ec.edu.uce.certificadorforense.core.rules.psd;

import ec.edu.uce.certificadorforense.core.rules.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

public class ReglaComplejidadDiseno implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        int totalCapas = psd.getCapas().size();

        // Contamos cuántas capas tienen "valor agregado"
        long conTrabajo = psd.getCapas().stream().filter(c ->
                c.isTieneMascaraCapa() || c.isTieneEfectos() || c.isEsClippingMask() ||
                        c.getTipo() == EstructuraCapaPSD.Tipo.TEXTO || !"norm".equals(c.getBlendModeKey()) ||
                        c.getOpacidadRaw() < 255 ||
                        // Consideramos "trabajo artístico" a una capa que no está vacía y cuyo contenido
                        // (bounding box) no abarca exactamente todo el lienzo (lo que indica que son trazos o dibujos).
                        (c.getAncho() > 0 && c.getAlto() > 0 && 
                        (c.getAncho() < psd.getMetadatos().getAnchoImagen() || c.getAlto() < psd.getMetadatos().getAltoImagen()))
        ).count();

        boolean esValido;
        String mensaje;

        if (totalCapas <= 3) {
            // Si tiene 3 o menos capas, EXIGIMOS que al menos 1 sea técnica
            esValido = conTrabajo >= 1;
            mensaje = "Pocas capas (" + totalCapas + "). Requiere al menos 1 capa técnica. Encontradas: " + conTrabajo;
        } else {
            // Si tiene más de 3, EXIGIMOS al menos 2 capas técnicas
            esValido = conTrabajo >= 2;
            mensaje = "Estructura compleja (" + totalCapas + " capas). Requiere al menos 2 capas técnicas. Encontradas: " + conTrabajo;
        }

        return ResultadoValidacion.builder()
                .nombreRegla("Autenticidad Técnica")
                .esValido(esValido)
                .mensaje(esValido ? "OK" : mensaje)
                .build();
    }

    @Override public boolean esCritica() { return true; } // DESCARTA AL INSTANTE
}
