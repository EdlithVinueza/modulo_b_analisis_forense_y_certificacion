package ec.edu.uce.certificadorforense.core.observer.listeners;

import ec.edu.uce.certificadorforense.core.observer.EventListener;
import ec.edu.uce.certificadorforense.core.observer.EventoSistema;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * Listener: se suscribe a TODOS los eventos del sistema y escribe un registro
 * de auditoría en {@code resultados certificacion/auditoria.log}.
 */
public class AuditoriaListener implements EventListener {

    private static final String DEFAULT_RUTA_LOG = "resultados_test_certificacion/auditoria.log";
    private final String rutaLog;

    public AuditoriaListener() {
        this(DEFAULT_RUTA_LOG);
    }

    public AuditoriaListener(String rutaLog) {
        this.rutaLog = rutaLog;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        // Registra todos los eventos sin excepción
        return true;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        String linea = "[AUDITORIA] " + Instant.now() + " | "
                + evento.getNombreEvento() + " | evento-ts: " + evento.getTimestamp();
        System.out.println(linea);

        try (PrintWriter pw = new PrintWriter(new FileWriter(this.rutaLog, true))) {
            pw.println(linea);
        } catch (IOException e) {
            System.err.println("[AuditoriaListener] No se pudo escribir en el log: " + e.getMessage());
        }
    }
}
