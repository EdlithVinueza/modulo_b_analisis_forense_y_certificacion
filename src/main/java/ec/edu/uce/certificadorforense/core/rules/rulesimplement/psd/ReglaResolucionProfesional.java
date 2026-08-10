package ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public class ReglaResolucionProfesional implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        double dpi = psd.getMetadatos().getDpiHorizontal();
        boolean pasa = dpi >= 150.0;
        return ResultadoValidacion.builder()
                .nombreRegla("Calidad de Impresión")
                .esValido(pasa)
                .mensaje(pasa ? "Resolución OK" : "Resolución muy baja: " + dpi + " DPI")
                .build();
    }
    @Override public boolean esCritica() { return false; }
}
