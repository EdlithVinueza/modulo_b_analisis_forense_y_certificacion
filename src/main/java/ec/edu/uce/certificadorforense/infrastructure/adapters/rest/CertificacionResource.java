package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import ec.edu.uce.certificadorforense.application.service.CertificacionOrchestrator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que actúa como punto de entrada de la UI en Vue.
 * Ahora delegando correctamente al Orquestador (Arquitectura Hexagonal).
 */
@Path("/api/v1/certificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificacionResource {

    @Inject
    CertificacionOrchestrator orchestrator;

    @POST
    @Path("/init")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response iniciarCertificacion(@RestForm("psd") FileUpload psdFile,
                                         @RestForm("imagen") FileUpload imagenFile) {
        try {
            if (psdFile == null || imagenFile == null) {
                return errorResponse("Faltan archivos para iniciar el análisis.");
            }

            // RESTEasy genera archivos sin extensión (ej. resteasy-reactive123upload).
            // Para que la regla de "Firma Estructural Forense" pase, el archivo debe terminar en .png/.psd
            java.nio.file.Path tempPsd = Files.createTempFile("forense-", "-" + psdFile.fileName());
            Files.copy(psdFile.uploadedFile(), tempPsd, StandardCopyOption.REPLACE_EXISTING);
            
            java.nio.file.Path tempImg = Files.createTempFile("forense-", "-" + imagenFile.fileName());
            Files.copy(imagenFile.uploadedFile(), tempImg, StandardCopyOption.REPLACE_EXISTING);
            
            File psd = tempPsd.toFile();
            File img = tempImg.toFile();
            
            // Detect extension from filename
            String ext = "png";
            if(imagenFile.fileName().toLowerCase().endsWith(".jpg") || imagenFile.fileName().toLowerCase().endsWith(".jpeg")) {
                ext = "jpg";
            }

            String expedienteId = orchestrator.iniciarAnalisisFase1(psd, img, ext);

            Map<String, String> response = new HashMap<>();
            response.put("expediente_id", expedienteId);
            response.put("estado", "ANALIZADO");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    @PUT
    @Path("/{id}/datos")
    public Response enviarDatosObra(@PathParam("id") String idExpediente, Map<String, Object> body) {
        try {
            String cedula = (String) body.get("cedula");
            
            // Verificar en la BD (Panache) si la cédula existe
            var usuario = ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity.find("cedula", cedula).firstResult();
            
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                       .entity("{\"error\": \"Ese número de cédula no se encuentra registrado en nuestro sistema.\"}")
                       .build();
            }
            
            orchestrator.registrarDatosFase2(idExpediente, (ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity) usuario, body);
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Datos guardados y vinculados correctamente en la base de datos.");
            response.put("estado", "ESPERANDO_FIRMA");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            String msg = e.getMessage();
            Throwable cause = e.getCause();
            while(cause != null) {
                msg += " | Causa: " + cause.getMessage();
                cause = cause.getCause();
            }
            return errorResponse(msg);
        }
    }

    @POST
    @Path("/{id}/firmar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response firmarExpediente(@PathParam("id") String idExpediente,
                                     @RestForm("certificado") FileUpload p12Upload,
                                     @RestForm("password") String password) {
        try {
            if (p12Upload == null || password == null || password.isEmpty()) {
                return errorResponse("Falta el certificado P12 o la contraseña.");
            }

            File p12File = p12Upload.uploadedFile().toFile();
            
            String hashCert = orchestrator.firmarFase3(idExpediente, p12File, password);

            Map<String, String> response = new HashMap<>();
            response.put("hash_certificado", hashCert);
            response.put("estado", "CERTIFICADO");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            // Vue interceptará este JSON para mostrar alertas estructuradas.
            return errorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/{id}/descargar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarCertificado(@PathParam("id") String idExpediente) {
        try {
            byte[] zipBytes = orchestrator.emitirCertificadoFase4(idExpediente);

            return Response.ok(zipBytes)
                    .header("Content-Disposition", "attachment; filename=\"Expediente_Forense_" + idExpediente + ".zip\"")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar el ZIP: " + e.getMessage())
                    .build();
        }
    }

    private Response errorResponse(String message) {
        String jsonError = "{\"error\": \"" + message.replace("\"", "\\\"").replace("\n", " ") + "\"}";
        return Response.status(Response.Status.BAD_REQUEST).entity(jsonError).build();
    }
}
