package ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public class ReglaFormatoPsd implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        boolean esPsd = "PSD".equalsIgnoreCase(psd.getMetadatos().getNombreArchivoDetectado());
        return ResultadoValidacion.builder()
                .nombreRegla("Formato de Archivo")
                .esValido(esPsd)
                .mensaje(esPsd ? "OK" : "No es un archivo PSD real")
                .build();
    }
    @Override public boolean esCritica() { return true; }
}
