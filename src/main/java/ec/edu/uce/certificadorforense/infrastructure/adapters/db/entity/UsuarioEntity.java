package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa un Usuario en el sistema.
 * Mapea la tabla "usuarios" en PostgreSQL.
 * Utiliza Panache (Active Record) para las operaciones de base de datos.
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    public String cedula;

    @Column(name = "cedula_hash", unique = true, columnDefinition = "TEXT")
    public String cedulaHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String nombres;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String apellidos;

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    public String correo;

    @Column(name = "nombre_artistico", columnDefinition = "TEXT")
    public String nombreArtistico;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    public String passwordHash;

    @Column(name = "firma_p12", columnDefinition = "TEXT")
    public String firmaP12;

    @Column(name = "acepta_terminos_plataforma")
    public Boolean aceptaTerminosPlataforma;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;

    public Boolean activo;

    @PrePersist
    void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }
}
