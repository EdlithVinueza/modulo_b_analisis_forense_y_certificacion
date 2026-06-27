package ec.edu.uce.certificadorforense.infrastructure.adapters.db;

import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.ExpedienteForenseEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.ObraEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de Infraestructura para guardar expedientes en PostgreSQL.
 * Implementa el puerto de salida del Core sin acoplar el dominio a Hibernate.
 */
@ApplicationScoped
public class ExpedienteDatabaseAdapter implements ExpedienteRepositoryPort {

    @Override
    @Transactional
    public void guardar(Expediente expediente, String expedienteJson, String firmaBase64) {
        // En una implementación real más compleja, aquí se buscaría la obra por su ID.
        // Para este prototipo, simularemos la creación de los registros relacionales si no existen.
        
        UsuarioEntity usuario = UsuarioEntity.find("correo", expediente.getAutor().getCorreo()).firstResult();
        if (usuario == null) {
            usuario = new UsuarioEntity();
            usuario.id = UUID.randomUUID();
            usuario.cedula = expediente.getAutor().getCedula();
            usuario.nombres = expediente.getAutor().getNombres();
            usuario.apellidos = expediente.getAutor().getApellidos();
            usuario.correo = expediente.getAutor().getCorreo();
            usuario.nombreArtistico = expediente.getAutor().getSeudonimo();
            usuario.passwordHash = "NO_AUTH_LOCAL";
            usuario.persist();
        }

        ObraEntity obra = new ObraEntity();
        obra.id = UUID.randomUUID();
        obra.usuario = usuario;
        obra.titulo = expediente.getObra().getTitulo();
        obra.descripcion = expediente.getObra().getDescripcion();
        obra.categoria = expediente.getObra().getCategoria().name();
        obra.software = expediente.getObra().getSoftware();
        obra.hardware = expediente.getObra().getHardware();
        obra.fechaRegistro = LocalDateTime.now();
        obra.estadoActual = "CERTIFICADO";
        obra.persist();

        ExpedienteForenseEntity entity = new ExpedienteForenseEntity();
        entity.id = UUID.randomUUID();
        entity.obra = obra;
        entity.hashPsdOriginal = expediente.getHashes().getSha512PSD();
        entity.hashImagenFinal = expediente.getHashes().getSha512Imagen();
        entity.similitudPhash = null; // Parsear si es necesario
        entity.resultadoAnalisis = expediente.getAnalisis().getResultado();
        entity.evidenciaTecnicaJson = expedienteJson;
        entity.fechaAnalisis = LocalDateTime.now();
        
        entity.persist();
        
        // (La firma también se guardaría en FirmasAutorEntity según el modelo propuesto, 
        // pero esto es suficiente para satisfacer el adaptador inicial).
    }

    @Override
    public Optional<Expediente> buscarPorId(String idExpediente) {
        // Aquí se reconstruiría el objeto de Dominio (Expediente) a partir de la BD.
        // Por ahora, se retorna Optional.empty() para mantener el código base simple en esta fase.
        return Optional.empty();
    }
}
