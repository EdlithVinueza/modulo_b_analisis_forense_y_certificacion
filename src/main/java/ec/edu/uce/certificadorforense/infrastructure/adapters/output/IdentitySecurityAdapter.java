package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import ec.edu.uce.certificadorforense.core.ports.out.FirmadorNubePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.Base64;

@ApplicationScoped
public class IdentitySecurityAdapter implements FirmadorNubePort {

    @Inject
    @RestClient
    SignatureClient signatureClient;

    @Override
    public String firmar(String p12Base64, String password, String hashObra) {
        // Se envía el hash generado a la CA de Mercedes (módulo-validar)
        JsonObject json = Json.createObjectBuilder()
                .add("p12Base64", p12Base64)
                .add("password", password)
                .add("hashObra", hashObra) // SHA-512
                .build();

        JsonObject response = signatureClient.signWork(json);

        if (response.containsKey("firmaDigital")) {
            return response.getString("firmaDigital");
        } else if (response.containsKey("firma")) {
            return response.getString("firma");
        }
        throw new FirmadorNubePort.FirmaNubeException(
                "No se encontró la firma digital en la respuesta de la CA.");
    }

    /** @deprecated Usa {@link #firmar(String, String, String)} vía el puerto. */
    @Deprecated
    public String getAuthorDigitalSignature(String p12Base64, String password, String hashObra) {
        return firmar(p12Base64, password, hashObra);
    }
}
