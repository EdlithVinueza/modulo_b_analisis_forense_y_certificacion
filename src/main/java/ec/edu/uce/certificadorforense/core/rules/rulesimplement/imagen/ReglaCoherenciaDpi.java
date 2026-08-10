package ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen;

import ec.edu.uce.certificadorforense.core.rules.rulesinterface.IReglaValidacion;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.ResultadoValidacion;

public class ReglaCoherenciaDpi implements IReglaValidacion<ArchivoImagen> {
    @Override
    public ResultadoValidacion validar(ArchivoImagen img) {
        double dpiLibreria = img.getMetadatos().getDpiCalculado();
        int dpiBinario = img.getEstructura().getDpiX();

        boolean coherente = Math.abs(dpiLibreria - dpiBinario) < 1.1;

        return ResultadoValidacion.builder()
                .nombreRegla("Coherencia de Resolución")
                .esValido(coherente)
                .mensaje(coherente ? "Metadatos y Estructura coinciden." :
                        "MANIPULACIÓN DETECTADA: Librería dice " + dpiLibreria + " pero los bytes reales son " + dpiBinario)
                .build();
    }
    @Override public boolean esCritica() { return true; }
}
