package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.Base64;

@ApplicationScoped
public class IdentitySecurityAdapter {

    @Inject
    @RestClient
    SignatureClient signatureClient;

    public String getAuthorDigitalSignature(String p12Base64, String password, String hashObra) {
        // Se envía el hash generado por Edlith a la infraestructura de Mercedes
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
        throw new RuntimeException("No se encontró la firma digital en la respuesta de la CA.");
    }
}
