package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true, length = 20)
    public String cedula;

    @Column(nullable = false, length = 100)
    public String nombres;

    @Column(nullable = false, length = 100)
    public String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    public String correo;

    @Column(name = "nombre_artistico", length = 100)
    public String nombreArtistico;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "acepta_terminos_plataforma")
    public Boolean aceptaTerminosPlataforma;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;

    public Boolean activo;

    @Column(name = "firma_p12", columnDefinition = "TEXT")
    public String firmaP12;
}
