package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.RecuperacionDatos;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — persistencia del expediente forense y todo lo que cuelga
 * de él (obra, declaraciones, firma, certificado, historial de estados).
 * <p>
 * Implementación única: {@code ExpedienteJpaRepositoryAdapter}.
 * </p>
 */
public interface ExpedienteRepositoryPort {

    // ── Fase 1 — detección de duplicados ──────────────────────────────
    Optional<ExpedienteResumen> buscarPorHashImagen(String hashImagen);

    Optional<ExpedienteResumen> buscarPorHashPsd(String hashPsd);

    /** Candidatos para comparación por similitud visual (pHash). */
    List<ExpedienteResumen> listarCertificadosConPHash();

    // ── Fase 2 — registro / rectificación de datos de la obra ─────────
    Optional<ExpedienteResumen> buscarResumenPorId(String idExpediente);

    /** Elimina un borrador huérfano (obra + expediente + declaraciones + firma/historial asociados). */
    void eliminarBorrador(UUID obraId);

    void registrarNuevaObraYExpediente(String idExpediente, UUID usuarioId, Obra obra, CategoriaObra categoria,
                                        Declaraciones declaraciones, String ipRegistro,
                                        String hashPsd, String hashImagen, String phash, String evidenciaTecnicaJson);

    void actualizarObraYDeclaraciones(String idExpediente, UUID usuarioId, Obra obra, CategoriaObra categoria,
                                       Declaraciones declaraciones, String ipRegistro);

    // ── Fase 3 — firma del autor ───────────────────────────────────────
    void guardarFirma(String idExpediente, FirmaAutor firma);

    // ── Fase 4 — emisión del certificado ───────────────────────────────
    void guardarCertificado(String idExpediente, Certificado certificado, String expedienteFirmadoJson);

    // ── Recuperación de un certificado ya emitido ──────────────────────
    Optional<RecuperacionDatos> buscarParaRecuperacion(String hashImagenODePsd);

    // ── Alertas de seguridad (registro en transacción independiente) ──
    void registrarAlertaSeguridad(UUID obraId, String cedulaIntento, String nombresIntento, String apellidosIntento);
}
