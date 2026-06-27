package ec.edu.uce.certificadorforense.infrastructure.adapters.rest;

import ec.edu.uce.certificadorforense.infrastructure.adapters.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Intercepta cualquier excepción arrojada por la lógica core (Fase 1 y Fase 3)
 * y la transforma en el JSON amigable (ErrorResponse) esperado por Vue.
 */
@Provider
public class ForenseExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // Aquí se puede añadir lógica para evaluar el tipo de excepción personalizada
        // (Ej: ArchivoNoSoportadoException, ReglaForenseFallidaException)
        
        String errorCode = "ERROR_FORENSE_INTERNO";
        String mensaje = "Ocurrió un error en la validación técnica.";
        int status = Response.Status.BAD_REQUEST.getStatusCode();
        
        if (exception.getMessage() != null && exception.getMessage().contains("PSD")) {
            errorCode = "CORRUPCION_PSD";
            mensaje = "El archivo maestro PSD parece estar dañado o corrupto.";
        } else if (exception.getMessage() != null && exception.getMessage().contains("Similitud")) {
            errorCode = "SIMILITUD_VISUAL_BAJA";
            mensaje = "Alerta Forense: La imagen exportada no coincide visualmente con las capas del PSD.";
            status = 422; // Unprocessable Entity
        }
        
        ErrorResponse errorResponse = new ErrorResponse(errorCode, mensaje, exception.getMessage());
        
        return Response.status(status).entity(errorResponse).build();
    }
}
