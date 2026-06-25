package ec.edu.uce.certificadorforense.core.ports.in;

import java.io.File;

/**
 * Puerto de entrada — Caso de uso: Iniciar Análisis Forense (Fase 1).
 * <p>
 * Recibe el archivo PSD y la imagen exportada, dispara el análisis forense
 * completo y almacena los resultados en el {@code ContextoProceso}.
 * </p>
 */
public interface IniciarAnalisisUseCase {

    /**
     * Ejecuta el análisis forense de los dos archivos.
     *
     * @param archivoPSD   Archivo PSD original de la obra.
     * @param archivoImagen Imagen exportada (PNG o JPG) de la obra.
     */
    void ejecutar(File archivoPSD, File archivoImagen);
}
