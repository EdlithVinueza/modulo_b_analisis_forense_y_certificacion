package ec.edu.uce.certificadorforense.core.model.expediente;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resumen de un expediente ya persistido, tal como lo necesita el orquestador
 * para detectar duplicados, validar propiedad y reconstruir datos en fases subsiguientes.
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
    private String usuarioNombres;
    private String usuarioApellidos;
    private String usuarioCorreo;
    private String usuarioNombreArtistico;
    private String obraTitulo;
    private String obraDescripcion;
    private String obraCategoria;
    private String obraSoftware;
    private String obraHardware;
    private LocalDate obraFechaCreacion;
}
