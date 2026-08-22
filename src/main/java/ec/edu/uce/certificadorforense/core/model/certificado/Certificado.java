package ec.edu.uce.certificadorforense.core.model.certificado;

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
public class Certificado {
    private String idCertificado;
    private String idExpediente;
    private String hashExpediente;
    private String hashExpedienteFirmado;
    private Instant fechaEmision;
    private String qrBase64;
    private String qrContenido;

    public String getHashExpedienteFirmado() {
        return hashExpedienteFirmado != null ? hashExpedienteFirmado : hashExpediente;
    }
}
