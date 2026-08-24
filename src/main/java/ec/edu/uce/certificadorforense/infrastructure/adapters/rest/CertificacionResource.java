package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import ec.edu.uce.certificadorforense.application.service.CertificacionOrchestrator;
import io.smallrye.common.annotation.Blocking;
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
 * Delegando al Orquestador (Arquitectura Hexagonal).
 * Todos los endpoints llevan @Blocking para evitar bloquear el Event Loop de Vert.x en RESTEasy Reactive.
 */
@Path("/api/v1/certificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificacionResource {

    @Inject
    CertificacionOrchestrator orchestrator;

    @Inject
    ec.edu.uce.certificadorforense.application.service.RecuperacionCertificadoService recuperacionService;

    @POST
    @Path("/init")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    public Response iniciarCertificacion(@RestForm("psd") FileUpload psdFile,
                                         @RestForm("imagen") FileUpload imagenFile) {
        java.nio.file.Path tempPsd = null;
        java.nio.file.Path tempImg = null;
        try {
            if (psdFile == null || imagenFile == null) {
                return errorResponse("Faltan archivos para iniciar el análisis.");
            }

            // RESTEasy genera archivos sin extensión. Para que la regla pase, se agrega sufijo.
            tempPsd = Files.createTempFile("forense-", "-" + psdFile.fileName());
            Files.copy(psdFile.uploadedFile(), tempPsd, StandardCopyOption.REPLACE_EXISTING);
            
            tempImg = Files.createTempFile("forense-", "-" + imagenFile.fileName());
            Files.copy(imagenFile.uploadedFile(), tempImg, StandardCopyOption.REPLACE_EXISTING);
            
            File psd = tempPsd.toFile();
            File img = tempImg.toFile();
            
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
        } finally {
            if (tempPsd != null) { try { Files.deleteIfExists(tempPsd); } catch (Exception ignored) {} }
            if (tempImg != null) { try { Files.deleteIfExists(tempImg); } catch (Exception ignored) {} }
        }
    }

    @POST
    @Path("/recuperar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/zip")
    @Blocking
    public Response recuperarCertificado(@RestForm("imagen") FileUpload imagenFile,
                                         @RestForm("hash_duplicado") String hashDuplicado,
                                         @RestForm("cedula") String cedula) {
        java.nio.file.Path tempImg = null;
        try {
            if (imagenFile == null || hashDuplicado == null || cedula == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Faltan datos para la recuperación (imagen, hash o cédula).")
                        .build();
            }

            tempImg = Files.createTempFile("forense-rec-", "-" + imagenFile.fileName());
            Files.copy(imagenFile.uploadedFile(), tempImg, StandardCopyOption.REPLACE_EXISTING);
            File img = tempImg.toFile();

            String ext = "png";
            if(imagenFile.fileName().toLowerCase().endsWith(".jpg") || imagenFile.fileName().toLowerCase().endsWith(".jpeg")) {
                ext = "jpg";
            }

            byte[] zipBytes = recuperacionService.recuperarCertificadoLocalmente(hashDuplicado, cedula, img, ext);

            return Response.ok(zipBytes, "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"Expediente_Recuperado.zip\"")
                    .header("Content-Length", zipBytes.length)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("ConflictoPropiedadException")) {
                return Response.status(Response.Status.CONFLICT)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(e.getMessage())
                        .build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Error en la recuperación: " + e.getMessage())
                    .build();
        } finally {
            if (tempImg != null) { try { Files.deleteIfExists(tempImg); } catch (Exception ignored) {} }
        }
    }

    @PUT
    @Path("/{id}/datos")
    @jakarta.transaction.Transactional
    @Blocking
    public Response enviarDatosObra(@PathParam("id") String idExpediente, Map<String, Object> body) {
        try {
            String cedula = (String) body.get("cedula");
            
            ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity usuario =
                    ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity.find("cedula", cedula).firstResult();

            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                       .entity("{\"error\": \"Ese número de cédula no se encuentra registrado en nuestro sistema.\"}")
                       .build();
            }

            ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos usuarioDatos =
                    ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos.builder()
                            .id(usuario.id)
                            .cedula(usuario.cedula)
                            .nombres(usuario.nombres)
                            .apellidos(usuario.apellidos)
                            .correo(usuario.correo)
                            .nombreArtistico(usuario.nombreArtistico)
                            .build();

            orchestrator.registrarDatosFase2(idExpediente, usuarioDatos, body);

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
    @Blocking
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
            return errorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/{id}/descargar")
    @Produces("application/zip")
    @Blocking
    public Response descargarCertificado(@PathParam("id") String idExpediente) {
        try {
            byte[] zipBytes = orchestrator.obtenerZipYLimpiar(idExpediente);

            if (zipBytes == null || zipBytes.length == 0) {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.APPLICATION_JSON)
                        .entity("{\"error\": \"El paquete ZIP no pudo ser generado o no existe para el expediente: " + idExpediente + "\"}")
                        .build();
            }

            return Response.ok(zipBytes, "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"Expediente_Forense_" + idExpediente + ".zip\"")
                    .header("Content-Length", zipBytes.length)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\": \"Error al generar el paquete ZIP: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private Response errorResponse(String message) {
        Map<String, String> errorPayload = new HashMap<>();
        errorPayload.put("error", message != null ? message : "Error interno desconocido");
        return Response.status(Response.Status.BAD_REQUEST).entity(errorPayload).build();
    }
}
