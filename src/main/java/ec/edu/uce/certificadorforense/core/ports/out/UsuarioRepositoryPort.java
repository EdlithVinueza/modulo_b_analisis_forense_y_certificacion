package ec.edu.uce.certificadorforense.core.ports.out;

import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import java.util.Optional;

/**
 * Puerto de salida para acceder a la identidad y datos del usuario sin acoplar
 * los controladores REST ni los casos de uso a las entidades JPA / Panache.
 */
public interface UsuarioRepositoryPort {

    Optional<UsuarioDatos> buscarPorCedula(String cedula);

    void flush();
}
