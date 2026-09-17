package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import ec.edu.uce.certificadorforense.application.service.RecuperacionCertificadoService;
import ec.edu.uce.certificadorforense.core.ports.in.EmitirCertificadoUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.FirmarExpedienteUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.IniciarAnalisisUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.RegistrarDatosObraUseCase;
import ec.edu.uce.certificadorforense.core.ports.out.UsuarioRepositoryPort;
import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import ec.edu.uce.certificadorforense.infrastructure.adapters.rest.util.TempFileUtil;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que actúa como punto de entrada de la UI en Vue.
 * Delegando directamente a los Casos de Uso (Puertos de Entrada de Arquitectura Hexagonal).
 */
@Path("/api/v1/certificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CertificacionResource {

    @Inject
    IniciarAnalisisUseCase iniciarAnalisisUseCase;

    @Inject
    RegistrarDatosObraUseCase registrarDatosObraUseCase;

    @Inject
    FirmarExpedienteUseCase firmarExpedienteUseCase;

    @Inject
    EmitirCertificadoUseCase emitirCertificadoUseCase;

    @Inject
    RecuperacionCertificadoService recuperacionService;

    @Inject
    JsonWebToken jwt;

    @Inject
    UsuarioRepositoryPort usuarioRepository;

    private Response forbidden(String message) {
        Map<String, String> errorPayload = new HashMap<>();
        errorPayload.put("error", message);
        return Response.status(Response.Status.FORBIDDEN).entity(errorPayload).build();
    }

    @POST
    @Path("/init")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    public Response iniciarCertificacion(@RestForm("psd") FileUpload psdFile,
                                         @RestForm("imagen") FileUpload imagenFile) {
        File psd = null;
        File img = null;
        try {
            if (psdFile == null || imagenFile == null) {
                return errorResponse("Faltan archivos para iniciar el análisis.");
            }

            psd = TempFileUtil.crearTemporal(psdFile, "forense-psd");
            img = TempFileUtil.crearTemporal(imagenFile, "forense-img");

            String ext = "png";
            if (imagenFile.fileName().toLowerCase().endsWith(".jpg") || imagenFile.fileName().toLowerCase().endsWith(".jpeg")) {
                ext = "jpg";
            }

            java.util.UUID usuarioId = null;
            String cedula = jwt.getClaim("cedula");
            if (cedula != null) {
                java.util.Optional<UsuarioDatos> usuarioOpt = usuarioRepository.buscarPorCedula(cedula);
                if (usuarioOpt.isPresent()) {
                    usuarioId = usuarioOpt.get().getId();
                }
            }

            Map<String, String> result = iniciarAnalisisUseCase.ejecutar(psd, img, ext, usuarioId);

            Map<String, String> response = new HashMap<>(result);
            response.putIfAbsent("estado", "ANALIZADO");

            return Response.ok(response).build();
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        } finally {
            TempFileUtil.borrarSilencioso(psd);
            TempFileUtil.borrarSilencioso(img);
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
        File img = null;
        try {
            if (imagenFile == null || hashDuplicado == null || cedula == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Faltan datos para la recuperación (imagen, hash o cédula).")
                        .build();
            }

            if (!cedula.equals(jwt.getClaim("cedula"))) {
                return Response.status(Response.Status.FORBIDDEN)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("No puedes recuperar el certificado de otro usuario.")
                        .build();
            }

            img = TempFileUtil.crearTemporal(imagenFile, "forense-rec");

            String ext = "png";
            if (imagenFile.fileName().toLowerCase().endsWith(".jpg") || imagenFile.fileName().toLowerCase().endsWith(".jpeg")) {
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
            TempFileUtil.borrarSilencioso(img);
        }
    }

    @PUT
    @Path("/{id}/datos")
    @jakarta.transaction.Transactional
    @Blocking
    public Response enviarDatosObra(@PathParam("id") String idExpediente, Map<String, Object> body) {
        try {
            String cedula = (String) body.get("cedula");

            if (!java.util.Objects.equals(cedula, jwt.getClaim("cedula"))) {
                return forbidden("No puedes registrar datos de obra a nombre de otro usuario.");
            }

            UsuarioDatos usuarioDatos = usuarioRepository.buscarPorCedula(cedula).orElse(null);

            if (usuarioDatos == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Ese número de cédula no se encuentra registrado en nuestro sistema.\"}")
                        .build();
            }

            registrarDatosObraUseCase.ejecutar(idExpediente, usuarioDatos, body);

            usuarioRepository.flush();

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Datos guardados y vinculados correctamente en la base de datos.");
            response.put("estado", "ESPERANDO_FIRMA");

            return Response.ok(response).build();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            Throwable cause = e.getCause();
            while (cause != null) {
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

            if (!emitirCertificadoUseCase.esPropietario(idExpediente, jwt.getClaim("cedula"))) {
                return forbidden("No puedes firmar el expediente de otro usuario.");
            }

            String hashCert = firmarExpedienteUseCase.ejecutar(idExpediente, password);

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
            if (!emitirCertificadoUseCase.esPropietario(idExpediente, jwt.getClaim("cedula"))) {
                return forbidden("No puedes descargar el certificado de otro usuario.");
            }

            byte[] zipBytes = emitirCertificadoUseCase.obtenerZipYLimpiar(idExpediente);

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
