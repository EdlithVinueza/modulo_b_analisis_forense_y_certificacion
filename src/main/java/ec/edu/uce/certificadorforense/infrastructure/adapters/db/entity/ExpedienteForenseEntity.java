package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "expedientes_forenses")
public class ExpedienteForenseEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false, unique = true)
    public ObraEntity obra;

    @Column(name = "hash_psd_original", nullable = false, unique = true, columnDefinition = "TEXT")
    public String hashPsdOriginal;

    @Column(name = "hash_imagen_final", nullable = false, columnDefinition = "TEXT")
    public String hashImagenFinal;

    @Column(name = "similitud_phash", precision = 5, scale = 2)
    public BigDecimal similitudPhash;

    @Column(name = "resultado_analisis", nullable = false, length = 50)
    public String resultadoAnalisis;

    @Column(name = "evidencia_tecnica", nullable = false, columnDefinition = "TEXT")
    public String evidenciaTecnicaJson;

    @Column(name = "fecha_analisis")
    public LocalDateTime fechaAnalisis;
}
