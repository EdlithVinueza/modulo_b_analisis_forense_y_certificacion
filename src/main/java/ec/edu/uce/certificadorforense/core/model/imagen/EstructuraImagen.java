package ec.edu.uce.certificadorforense.core.model.imagen;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstructuraImagen {
    private String formatoReal;
    private int dpiX;
    private int dpiY;
}
