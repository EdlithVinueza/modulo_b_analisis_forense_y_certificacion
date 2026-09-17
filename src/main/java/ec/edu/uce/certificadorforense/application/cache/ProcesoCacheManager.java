package ec.edu.uce.certificadorforense.application.cache;

import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor autónomo de caché en memoria para el análisis de archivos PSD.
 * Permite reutilizar los resultados de análisis estructural y pHash para el mismo archivo.
 */
@ApplicationScoped
public class ProcesoCacheManager {

    private static final Duration TTL_CACHE = Duration.ofHours(2);

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

    public PsdCacheEntry obtenerPsd(String sha512PSD) {
        return cachePsdAnalysis.get(sha512PSD);
    }

    public void guardarPsd(String sha512PSD, ArchivoPSD psd, String pHash) {
        cachePsdAnalysis.put(sha512PSD, new PsdCacheEntry(psd, pHash));
    }

    @Scheduled(every = "30m")
    void limpiarCachesExpiradas() {
        Instant limite = Instant.now().minus(TTL_CACHE);
        cachePsdAnalysis.entrySet().removeIf(entry -> entry.getValue().creadoEn.isBefore(limite));
    }
}
