package ec.edu.uce.certificadorforense.core.ports.in;

/**
 * Puerto de entrada — Caso de uso: Firmar Expediente (Fase 3).
 */
public interface FirmarExpedienteUseCase {
    String ejecutar(String idExpediente, String password) throws Exception;
}
