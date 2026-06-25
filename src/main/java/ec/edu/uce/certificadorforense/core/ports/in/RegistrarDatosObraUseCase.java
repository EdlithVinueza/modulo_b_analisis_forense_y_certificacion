package ec.edu.uce.certificadorforense.core.ports.in;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;

/**
 * Puerto de entrada — Caso de uso: Registrar Datos de la Obra (Fase 2).
 * <p>
 * Recibe los datos del autor, la obra y las declaraciones aceptadas.
 * </p>
 */
public interface RegistrarDatosObraUseCase {

    /**
     * Registra los datos de autor y obra en el contexto del proceso.
     *
     * @param autor         Datos del autor.
     * @param obra          Datos descriptivos de la obra.
     * @param declaraciones Las tres declaraciones obligatorias.
     */
    void ejecutar(Autor autor, Obra obra, Declaraciones declaraciones);
}
