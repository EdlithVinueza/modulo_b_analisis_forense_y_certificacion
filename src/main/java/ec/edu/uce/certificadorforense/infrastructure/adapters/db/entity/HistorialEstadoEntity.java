package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_estados")
public class HistorialEstadoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false)
    public ObraEntity obra;

    @Column(name = "estado_anterior", length = 50)
    public String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 50)
    public String estadoNuevo;

    @Column(columnDefinition = "TEXT")
    public String observacion;

    @Column(name = "fecha_cambio", nullable = false)
    public LocalDateTime fechaCambio;
}
