package ec.edu.uce.certificadorforense.core.ports.in;

import java.io.File;
import java.util.Map;

/**
 * Puerto de entrada — Caso de uso: Iniciar Análisis Forense (Fase 1).
 */
public interface IniciarAnalisisUseCase {
    Map<String, String> ejecutar(File archivoPSD, File archivoImagen, String extension) throws Exception;
    Map<String, String> ejecutar(File archivoPSD, File archivoImagen, String extension, java.util.UUID usuarioId) throws Exception;
}
