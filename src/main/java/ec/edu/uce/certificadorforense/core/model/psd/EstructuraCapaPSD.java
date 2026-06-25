package ec.edu.uce.certificadorforense.core.model.psd;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstructuraCapaPSD {
    public enum Tipo { NORMAL, GRUPO, AJUSTE, RELLENO, TEXTO, SMART_OBJECT, DESCONOCIDO }

    private final int indice;
    private final String nombre;
    private final Tipo tipo;
    private final String blendModeKey;
    private final String blendModeNombre;
    private final int opacidadRaw; // 0-255
    private final boolean visible;
    private final boolean bloqueada;
    private final boolean esClippingMask;
    private final boolean tieneMascaraCapa;
    private final boolean tieneEfectos;
    private final int ancho;
    private final int alto;
    private final int offsetX;
    private final int offsetY;
}
