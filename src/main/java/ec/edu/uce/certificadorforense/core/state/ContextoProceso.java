package ec.edu.uce.certificadorforense.core.state;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import lombok.Getter;
import lombok.Setter;

/**
 * Objeto de contexto compartido entre todos los estados del proceso de certificación.
 * <p>
 * Actúa como acumulador de datos a lo largo de las 4 fases.
 * El estado actual ({@link #estadoActual}) se actualiza con cada transición.
 * </p>
 *
 * <pre>
 * FASE 1 → sha512PSD, sha512Imagen, pHash, veredictoPSD, veredictoImagen
 * FASE 2 → autor, obra, declaraciones
 * FASE 3 → expediente, expedienteJson, firmaAutor
 * FASE 4 → certificado, pdfCertificado, imagenCertificada
 * </pre>
 */
@Getter
@Setter
public class ContextoProceso {

    // ── ESTADO ACTUAL ────────────────────────────────────────────────────────
    private EstadoProceso estadoActual;

    // ── FASE 1: Archivos de entrada ──────────────────────────────────────────
    private ArchivoPSD archivoPSD;
    private ArchivoImagen archivoImagen;

    // ── FASE 1: Resultados del análisis forense ──────────────────────────────
    private byte[] imagenRaw;
    private VeredictoFinal veredictoPSD;
    private VeredictoFinal veredictoImagen;
    private String sha512PSD;
    private String sha512Imagen;
    private String pHash;
    private int capasPSD;
    private boolean metadatosDetectados;

    // ── FASE 2: Datos de autor y obra ────────────────────────────────────────
    private Autor autor;
    private Obra obra;
    private Declaraciones declaraciones;

    // ── FASE 3: Expediente y firma del autor ─────────────────────────────────
    private Expediente expediente;
    private String expedienteJson;
    private FirmaAutor firmaAutor;

    // ── FASE 4: Certificado y archivos de salida ─────────────────────────────
    private Certificado certificado;
    private byte[] pdfCertificado;
    private byte[] imagenCertificada;
    private byte[] zipGenerado;

    /**
     * Crea un contexto vacío con el estado inicial: {@link AnalisisForenseState}.
     */
    public ContextoProceso(EstadoProceso estadoInicial) {
        this.estadoActual = estadoInicial;
    }
}
