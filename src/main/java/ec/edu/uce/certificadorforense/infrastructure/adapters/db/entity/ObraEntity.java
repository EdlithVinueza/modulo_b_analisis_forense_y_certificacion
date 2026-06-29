package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa una Obra registrada en el sistema.
 * Utiliza Panache (Active Record) para las operaciones de base de datos.
 */
@Entity
@Table(name = "obras")
public class ObraEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public UsuarioEntity usuario;

    @Column(nullable = false)
    public String titulo;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column(length = 50)
    public String categoria;

    @Column(length = 120)
    public String software;

    @Column(length = 120)
    public String hardware;

    @Column(name = "fecha_creacion")
    public LocalDate fechaCreacion;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;

    @Column(name = "estado_actual", nullable = false, length = 50)
    public String estadoActual;
}
