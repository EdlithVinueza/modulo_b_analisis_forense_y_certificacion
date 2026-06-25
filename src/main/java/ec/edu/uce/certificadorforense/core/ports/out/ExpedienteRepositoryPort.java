package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;

import java.util.Optional;

/**
 * Puerto de salida — Persistencia del expediente.
 * <p>
 * Implementación actual: {@code ExpedienteJsonAdapter} (archivo JSON local).
 * Implementación futura: adaptador PostgreSQL (cuando se elija el framework REST).
 * </p>
 */
public interface ExpedienteRepositoryPort {

    /**
     * Persiste el expediente y su JSON firmado.
     *
     * @param expediente       El expediente a guardar.
     * @param expedienteJson   JSON serializado del expediente (sin firma).
     * @param firmaBase64      Firma del autor en Base64.
     */
    void guardar(Expediente expediente, String expedienteJson, String firmaBase64);

    /**
     * Busca un expediente por su ID.
     *
     * @param idExpediente ID del expediente (formato {@code EXP-YYYY-NNNNNN}).
     * @return El expediente si existe.
     */
    Optional<Expediente> buscarPorId(String idExpediente);
}
