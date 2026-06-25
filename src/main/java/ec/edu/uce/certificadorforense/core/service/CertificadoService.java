package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorQRPort;

import java.time.Instant;
import java.time.Year;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio de dominio: generación del {@link Certificado}.
 * <p>
 * Genera el ID del certificado, el QR y construye el objeto {@link Certificado}
 * que será usado para producir el PDF en la Fase 4.
 * </p>
 */
public class CertificadoService {

    /** Tamaño del QR en píxeles. */
    private static final int QR_SIZE = 150;

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    private final GeneradorQRPort generadorQR;
    private final GeneradorHashPort generadorHash;

    public CertificadoService(GeneradorQRPort generadorQR, GeneradorHashPort generadorHash) {
        this.generadorQR = generadorQR;
        this.generadorHash = generadorHash;
    }

    /**
     * Genera el certificado completo.
     *
     * @param expediente            Expediente asociado.
     * @param expedienteFirmadoJson JSON del expediente ya firmado.
     * @return {@link Certificado} con ID, QR y hashes.
     */
    public Certificado generar(ec.edu.uce.certificadorforense.core.model.expediente.Expediente expediente, String expedienteFirmadoJson) {
        String idCertificado = generarId();

        // Contenido del QR con datos solicitados
        String nombreAutor = expediente.getAutor().getSeudonimo() != null && !expediente.getAutor().getSeudonimo().trim().isEmpty() 
            ? expediente.getAutor().getSeudonimo() 
            : expediente.getAutor().getNombreCompleto();
            
        String qrContenido = String.format("Certificado: %s\nExpediente: %s\nObra: %s\nAutor: %s",
                idCertificado,
                expediente.getIdExpediente(),
                expediente.getObra().getTitulo(),
                nombreAutor);

        byte[] qrBytes = generadorQR.generar(qrContenido, QR_SIZE, QR_SIZE);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);

        // Hash del expediente firmado
        String hashExpedienteFirmado = generadorHash.calcularSHA512(expedienteFirmadoJson);

        System.out.println("[CertificadoService] Certificado generado: " + idCertificado);

        return Certificado.builder()
                .idCertificado(idCertificado)
                .idExpediente(expediente.getIdExpediente())
                .fechaEmision(Instant.now())
                .hashExpedienteFirmado(hashExpedienteFirmado)
                .qrContenido(qrContenido)
                .qrBase64(qrBase64)
                .build();
    }

    /**
     * Genera un ID único para el certificado.
     * Formato: {@code CERT-YYYY-NNNNNN} (ej. {@code CERT-2026-000001}).
     */
    private String generarId() {
        int anio = Year.now().getValue();
        int numero = CONTADOR.getAndIncrement();
        return String.format("CERT-%d-%06d", anio, numero);
    }
}
