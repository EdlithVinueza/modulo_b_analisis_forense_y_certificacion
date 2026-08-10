package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HashesEvidencia {
    private final String sha512PSD;
    private final String sha512Imagen;
    private final String pHash;
}
