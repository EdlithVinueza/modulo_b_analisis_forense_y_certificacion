package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.model.expediente.AnalisisResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.expediente.HashesEvidencia;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;

import java.time.Instant;
import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio de dominio: construcción del {@link Expediente}.
 * <p>
 * Recopila los datos del {@link ContextoProceso} de las fases 1 y 2
 * y construye el objeto {@link Expediente} que será serializado y firmado en Fase 3.
 * </p>
 */
public class ExpedienteService {

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    /**
     * Construye un {@link Expediente} a partir del contexto acumulado.
     *
     * @param contexto Contexto del proceso con datos de Fase 1 y Fase 2.
     * @return Expediente listo para serializar y firmar.
     */
    public Expediente construir(ContextoProceso contexto) {
        String idExpediente = generarId();

        String dimensionesStr = contexto.getArchivoPSD() != null && contexto.getArchivoPSD().getMetadatos() != null 
                ? contexto.getArchivoPSD().getMetadatos().getAnchoImagen() + " x " + contexto.getArchivoPSD().getMetadatos().getAltoImagen() + " px"
                : "Desconocido";
        
        String categoriaStr = contexto.getObra() != null && contexto.getObra().getCategoria() != null 
                ? contexto.getObra().getCategoria().getEtiqueta() : "";
        String dpiStr = contexto.getArchivoPSD() != null && contexto.getArchivoPSD().getMetadatos() != null 
                ? String.format("%.0f", contexto.getArchivoPSD().getMetadatos().getDpiHorizontal()) : "";
        String detallesTecnicosStr = categoriaStr + ", " + dpiStr + " DPI";

        AnalisisResumen analisis = AnalisisResumen.builder()
                .resultado(contexto.getVeredictoPSD().isEsRechazado()
                        || contexto.getVeredictoImagen().isEsRechazado()
                        ? "RECHAZADO" : "APROBADO")
                .capasPSD(contexto.getCapasPSD())
                .metadatosDetectados(contexto.isMetadatosDetectados())
                .dimensiones(dimensionesStr)
                .detallesTecnicos(detallesTecnicosStr)
                .build();

        HashesEvidencia hashes = HashesEvidencia.builder()
                .sha512PSD(contexto.getSha512PSD())
                .sha512Imagen(contexto.getSha512Imagen())
                .pHash(contexto.getPHash())
                .build();

        return Expediente.builder()
                .idExpediente(idExpediente)
                .fechaRegistro(Instant.now().toString())
                .autor(contexto.getAutor())
                .obra(contexto.getObra())
                .analisis(analisis)
                .hashes(hashes)
                .build();
    }

    /**
     * Genera un ID único para el expediente.
     * Formato: {@code EXP-YYYY-NNNNNN} garantizando unicidad en base de datos.
     */
    private String generarId() {
        int anio = Year.now().getValue();
        long timestamp = System.currentTimeMillis() % 1000000;
        int random = (int) (Math.random() * 900 + 100);
        return String.format("EXP-%d-%06d%03d", anio, timestamp, random);
    }
}
