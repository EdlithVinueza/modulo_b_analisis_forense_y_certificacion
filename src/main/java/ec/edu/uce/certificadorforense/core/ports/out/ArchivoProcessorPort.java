package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import java.io.File;

/**
 * Puerto de salida — Procesamiento genérico de archivos para extracción de datos.
 * <p>
 * Define el contrato para procesadores específicos de tipos de archivo (ej. PSD, PNG, JPEG).
 * </p>
 *
 * @param <T> El tipo específico de ArchivoBase que este procesador retorna.
 */
public interface ArchivoProcessorPort<T extends ArchivoBase> {
    
    /**
     * Procesa el archivo y extrae toda su estructura y metadatos.
     *
     * @param file Archivo físico a procesar.
     * @return El modelo de dominio con la información extraída.
     */
    T procesar(File file);

    /**
     * Verifica si este procesador puede manejar el archivo dado.
     *
     * @param file Archivo físico a verificar.
     * @return true si es soportado, false en caso contrario.
     */
    boolean soporta(File file);
}
