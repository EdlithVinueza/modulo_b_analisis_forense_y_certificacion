package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import ec.edu.uce.certificadorforense.application.service.CertificacionOrchestrator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.vertx.core.http.HttpServerRequest;
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

    @Inject
    ec.edu.uce.certificadorforense.application.service.RecuperacionCertificadoService recuperacionService;

    @Inject
    HttpServerRequest request;

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

            Map<String, String> result = orchestrator.iniciarAnalisisFase1(psd, img, ext);

            Map<String, String> response = new HashMap<>(result);
            response.putIfAbsent("estado", "ANALIZADO");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    @POST
    @Path("/recuperar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response recuperarCertificado(@RestForm("imagen") FileUpload imagenFile,
                                         @RestForm("hash_duplicado") String hashDuplicado,
                                         @RestForm("cedula") String cedula) {
        try {
            if (imagenFile == null || hashDuplicado == null || cedula == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Faltan datos para la recuperación (imagen, hash o cédula).")
                        .build();
            }

            java.nio.file.Path tempImg = Files.createTempFile("forense-rec-", "-" + imagenFile.fileName());
            Files.copy(imagenFile.uploadedFile(), tempImg, StandardCopyOption.REPLACE_EXISTING);
            File img = tempImg.toFile();

            String ext = "png";
            if(imagenFile.fileName().toLowerCase().endsWith(".jpg") || imagenFile.fileName().toLowerCase().endsWith(".jpeg")) {
                ext = "jpg";
            }

            byte[] zipBytes = recuperacionService.recuperarCertificadoLocalmente(hashDuplicado, cedula, img, ext);

            // Borrar archivo temporal
            try { Files.deleteIfExists(img.toPath()); } catch (Exception ignored) {}

            return Response.ok(zipBytes)
                    .header("Content-Disposition", "attachment; filename=\"Expediente_Recuperado.zip\"")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("ConflictoPropiedadException")) {
                return Response.status(Response.Status.CONFLICT)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(e.getMessage())
                        .build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Error en la recuperación: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}/datos")
    @jakarta.transaction.Transactional
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

            String ipCliente = request.getHeader("X-Forwarded-For");
            if (ipCliente == null || ipCliente.isEmpty()) {
                ipCliente = request.remoteAddress() != null ? request.remoteAddress().host() : "127.0.0.1";
            }
            if (ipCliente != null && ipCliente.contains(",")) {
                ipCliente = ipCliente.split(",")[0].trim();
            }
            body.put("ip_registro", ipCliente);

            orchestrator.registrarDatosFase2(idExpediente, (ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity) usuario, body);
            
            // Forzar persistencia para capturar cualquier error de SQL de inmediato
            ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity.getEntityManager().flush();
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Datos guardados y vinculados correctamente en la base de datos.");
            response.put("estado", "ESPERANDO_FIRMA");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            Throwable cause = e.getCause();
            while(cause != null) {
                msg += " | Causa: " + (cause.getMessage() != null ? cause.getMessage() : cause.toString());
                cause = cause.getCause();
            }
            return errorResponse(msg);
        }
    }

    @POST
    @Path("/{id}/firmar")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response firmarExpediente(@PathParam("id") String idExpediente,
                                     @RestForm("password") String password) {
        try {
            if (password == null || password.isEmpty()) {
                return errorResponse("Falta la contraseña.");
            }

            String hashCert = orchestrator.firmarFase3(idExpediente, password);

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
            byte[] zipBytes = orchestrator.obtenerZipYLimpiar(idExpediente);

            return Response.ok(zipBytes)
                    .header("Content-Disposition", "attachment; filename=\"Expediente_Forense_" + idExpediente + ".zip\"")
                    .build();
        } catch (Exception e) {
            e.printStackTrace(); // Log the exact error to the terminal
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Error al generar el ZIP: " + e.getMessage())
                    .build();
        }
    }

    private Response errorResponse(String message) {
        Map<String, String> errorPayload = new HashMap<>();
        errorPayload.put("error", message != null ? message : "Error interno desconocido");
        return Response.status(Response.Status.BAD_REQUEST).entity(errorPayload).build();
    }
}
