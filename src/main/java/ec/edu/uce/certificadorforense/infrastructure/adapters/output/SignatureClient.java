package ec.edu.uce.certificadorforense.infrastructure.adapters.output;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/api")
@RegisterRestClient(configKey = "signature-api")
// Llama al método getApiKey para inyectar la llave
@ClientHeaderParam(name = "x-functions-key", value = "{getApiKey}")
public interface SignatureClient {

    default String getApiKey() {
        return ConfigProvider.getConfig().getOptionalValue("azure.function.key", String.class).orElse("");
    }

    @POST
    @Path("/firmar_obra")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject signWork(JsonObject json);
}
