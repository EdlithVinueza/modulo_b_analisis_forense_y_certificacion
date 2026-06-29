package ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificados")
public class CertificadoEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false, unique = true)
    public ObraEntity obra;

    @Column(name = "numero_certificado", nullable = false, unique = true, length = 100)
    public String numeroCertificado;

    @Column(name = "expediente_firmado_raw", nullable = false, columnDefinition = "TEXT")
    public String expedienteFirmadoRaw;

    @Column(name = "hash_certificado", nullable = false, columnDefinition = "TEXT")
    public String hashCertificado;

    @Column(name = "ruta_pdf_nube", nullable = false, columnDefinition = "TEXT")
    public String rutaPdfNube;

    @Column(name = "ruta_png_nube", columnDefinition = "TEXT")
    public String rutaPngNube;

    @Column(name = "fecha_emision", nullable = false)
    public LocalDateTime fechaEmision;
}
