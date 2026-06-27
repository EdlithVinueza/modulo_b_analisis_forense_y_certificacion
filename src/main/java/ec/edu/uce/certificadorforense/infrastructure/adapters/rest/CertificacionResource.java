package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST que actúa como punto de entrada de la UI en Vue.
 * Delegará el procesamiento real a los casos de uso en el Core (Hexagonal).
 */
@Path("/api/v1/certificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificacionResource {

    // (Aquí se inyectaría la interfaz del Caso de Uso del Módulo B usando @Inject)

    @POST
    @Path("/init")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response iniciarCertificacion(/* MultipartFormDataInput input */) {
        // Simulación: Recibe archivos PSD y PNG desde Vue, los analiza (Fase 1)
        // y devuelve el ID para el siguiente paso.
        Map<String, String> response = new HashMap<>();
        response.put("expediente_id", UUID.randomUUID().toString());
        response.put("estado", "ANALIZADO");
        
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}/datos")
    public Response enviarDatosObra(@PathParam("id") String idExpediente /*, DatosObraDTO dto */) {
        // Simulación: Recibe título, EULA (Fase 2) y une con el expediente.
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Datos guardados correctamente.");
        response.put("estado", "ESPERANDO_FIRMA");
        
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/firmar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response firmarExpediente(@PathParam("id") String idExpediente /*, MultipartFormDataInput input */) {
        // Simulación: Recibe P12, lo descifra y sella el JSON (Fase 3).
        Map<String, String> response = new HashMap<>();
        response.put("hash_certificado", "a1b2c3d4e5f6...simulado");
        response.put("estado", "CERTIFICADO");
        
        return Response.ok(response).build();
    }
}
