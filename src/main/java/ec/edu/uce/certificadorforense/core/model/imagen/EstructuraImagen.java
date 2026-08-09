package ec.edu.uce.certificadorforense.core.model.imagen;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstructuraImagen {
    private String formatoReal;      // PNG o JPEG
    private String firmaHex;         // Los primeros bytes en formato hexadecimal
    private int dpiX;
    private int dpiY;
    private boolean tieneResolucionFisica;
}

