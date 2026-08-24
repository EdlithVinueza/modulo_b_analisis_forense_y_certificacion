package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "declaraciones_obra")
public class DeclaracionesObraEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false, unique = true)
    public ObraEntity obra;

    @Column(name = "es_titular_derechos", nullable = false)
    public Boolean esTitularDerechos;



    @Column(name = "acepta_terminos_certificacion", nullable = false)
    public Boolean aceptaTerminosCertificacion;

    @Column(name = "fecha_aceptacion")
    public LocalDateTime fechaAceptacion;
}
