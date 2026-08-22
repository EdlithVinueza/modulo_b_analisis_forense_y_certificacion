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
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@Path("/api")
@RegisterRestClient(configKey = "signature-api")
// Llama al método getApiKey para inyectar la llave
@ClientHeaderParam(name = "x-functions-key", value = "{getApiKey}")
public interface SignatureClient {

    default String getApiKey() {
        return ConfigProvider.getConfig().getOptionalValue("azure.function.key", String.class).orElse("");
    }

    // Sin esto, si el simulador de CA se cuelga, la firma de un expediente se
    // queda esperando indefinidamente. Timeout + reintentos acotados evitan que
    // un fallo transitorio del servicio externo bloquee la request para siempre.
    @POST
    @Path("/firmar_obra")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(15000)
    @Retry(maxRetries = 2, delay = 500)
    JsonObject signWork(JsonObject json);
}
