package ec.edu.uce.certificadorforense.infrastructure.adapters.db;

import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import ec.edu.uce.certificadorforense.core.ports.out.UsuarioRepositoryPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.security.BlindIndexService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Adaptador de persistencia JPA para UsuarioRepositoryPort.
 * Aísla completamente el acceso a UsuarioEntity y BlindIndexService
 * de los controladores REST y casos de uso.
 */
@ApplicationScoped
public class UsuarioJpaRepositoryAdapter implements UsuarioRepositoryPort {

    @Inject
    BlindIndexService blindIndexService;

    @Override
    public Optional<UsuarioDatos> buscarPorCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return Optional.empty();
        }

        String hash = blindIndexService.hash(cedula);
        UsuarioEntity usuario = UsuarioEntity.find("cedulaHash", hash).firstResult();
        if (usuario == null) {
            return Optional.empty();
        }

        return Optional.of(UsuarioDatos.builder()
                .id(usuario.id)
                .cedula(usuario.cedula)
                .nombres(usuario.nombres)
                .apellidos(usuario.apellidos)
                .correo(usuario.correo)
                .nombreArtistico(usuario.nombreArtistico)
                .build());
    }

    @Override
    public void flush() {
        UsuarioEntity.getEntityManager().flush();
    }
}
