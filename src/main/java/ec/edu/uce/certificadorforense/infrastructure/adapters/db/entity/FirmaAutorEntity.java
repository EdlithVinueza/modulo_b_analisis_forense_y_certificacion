package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "firmas_autor")
public class FirmaAutorEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false, unique = true)
    public ExpedienteForenseEntity expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public UsuarioEntity usuario;

    @Column(name = "hash_firmado", nullable = false, columnDefinition = "TEXT")
    public String hashFirmado;

    @Column(name = "firma_base64", nullable = false, columnDefinition = "TEXT")
    public String firmaBase64;

    @Column(nullable = false, length = 50)
    public String algoritmo;

    @Column(name = "fecha_firma", nullable = false)
    public LocalDateTime fechaFirma;
}
