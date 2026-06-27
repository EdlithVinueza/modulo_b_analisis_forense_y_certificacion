package ec.edu.uce.certificadorforense.infrastructure.adapters.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Estructura estándar de error JSON para ser consumida por el frontend en Vue.
 */
@RegisterForReflection
public class ErrorResponse {
    
    public String errorCode;
    public String mensajeUsuario;
    public String detalleTecnico;

    public ErrorResponse(String errorCode, String mensajeUsuario, String detalleTecnico) {
        this.errorCode = errorCode;
        this.mensajeUsuario = mensajeUsuario;
        this.detalleTecnico = detalleTecnico;
    }
}
