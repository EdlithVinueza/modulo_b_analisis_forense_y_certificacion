package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Resumen de un expediente ya persistido, tal como lo necesita el orquestador
 * para detectar duplicados y validar propiedad — sin acoplarse a la entidad JPA.
 */
@Getter
@Builder
public class ExpedienteResumen {
    private UUID idExpediente;
    private UUID obraId;
    private String estadoActual;
    private String hashPsdOriginal;
    private String hashImagenFinal;
    private String phashImagenString;
    private String evidenciaTecnicaJson;
    private UUID usuarioId;
    private String usuarioCedula;
    private String usuarioFirmaP12;
}
