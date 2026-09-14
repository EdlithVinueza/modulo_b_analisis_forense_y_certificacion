package ec.edu.uce.certificadorforense.application.cache;

import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor autónomo de caché en memoria para las sesiones de certificación
 * y análisis PSD desacoplado del orquestador.
 */
@ApplicationScoped
public class ProcesoCacheManager {

    private static final Duration TTL_CACHE = Duration.ofHours(2);

    private final Map<String, ContextoProceso> contextoCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> contextoCreadoEn = new ConcurrentHashMap<>();
    private final Map<String, PsdCacheEntry> cachePsdAnalysis = new ConcurrentHashMap<>();

    public static class PsdCacheEntry {
        public final ArchivoPSD psd;
        public final String pHash;
        public final Instant creadoEn = Instant.now();

        public PsdCacheEntry(ArchivoPSD psd, String pHash) {
            this.psd = psd;
            this.pHash = pHash;
        }
    }

    public void guardarContexto(String expedienteId, ContextoProceso contexto) {
        contextoCache.put(expedienteId, contexto);
        contextoCreadoEn.put(expedienteId, Instant.now());
    }

    public ContextoProceso obtenerContexto(String expedienteId) {
        return contextoCache.get(expedienteId);
    }

    public ContextoProceso removerContexto(String expedienteId) {
        contextoCreadoEn.remove(expedienteId);
        return contextoCache.remove(expedienteId);
    }

    public PsdCacheEntry obtenerPsd(String sha512PSD) {
        return cachePsdAnalysis.get(sha512PSD);
    }

    public void guardarPsd(String sha512PSD, ArchivoPSD psd, String pHash) {
        cachePsdAnalysis.put(sha512PSD, new PsdCacheEntry(psd, pHash));
    }

    @Scheduled(every = "30m")
    void limpiarCachesExpiradas() {
        Instant limite = Instant.now().minus(TTL_CACHE);

        contextoCreadoEn.entrySet().removeIf(entry -> {
            boolean expirado = entry.getValue().isBefore(limite);
            if (expirado) {
                contextoCache.remove(entry.getKey());
            }
            return expirado;
        });

        cachePsdAnalysis.entrySet().removeIf(entry -> entry.getValue().creadoEn.isBefore(limite));
    }
}
