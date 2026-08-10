package ec.edu.uce.certificadorforense.core.model.firma;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmaAutor {
    private String idFirma;
    private String idExpediente;
    private String hashExpediente;
    private String firmaBase64;
    private Instant fechaFirma;
    private String certificadoBase64;
    private String algoritmo;
    private String aliasKeystore;
}
