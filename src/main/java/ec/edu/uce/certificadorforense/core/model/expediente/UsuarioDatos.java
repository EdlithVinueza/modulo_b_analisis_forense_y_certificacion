package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Datos del usuario que el orquestador necesita para registrar/rectificar un
 * expediente — lo que la capa REST extrae de UsuarioEntity antes de cruzar
 * hacia application/service, para que esa capa no dependa de infraestructura.
 */
@Getter
@Builder
public class UsuarioDatos {
    private UUID id;
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private String nombreArtistico;
}
