package ec.edu.uce.certificadorforense.core.ports.in;

import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import java.util.Map;

/**
 * Puerto de entrada — Caso de uso: Registrar Datos de la Obra (Fase 2).
 */
public interface RegistrarDatosObraUseCase {
    void ejecutar(String idExpediente, UsuarioDatos usuarioDb, Map<String, Object> body);
}
