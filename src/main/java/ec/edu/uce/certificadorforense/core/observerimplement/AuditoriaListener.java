package ec.edu.uce.certificadorforense.core.observerimplement;

import ec.edu.uce.certificadorforense.core.observerinterface.EventListener;
import ec.edu.uce.certificadorforense.core.observerinterface.EventoSistema;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * Listener: registra eventos de auditoría (opcionalmente a un archivo si se especifica en tests).
 */
public class AuditoriaListener implements EventListener {

    private final String rutaLog;

    public AuditoriaListener() {
        this(null);
    }

    public AuditoriaListener(String rutaLog) {
        this.rutaLog = rutaLog;
    }

    @Override
    public boolean soporta(EventoSistema evento) {
        return true;
    }

    @Override
    public void onEvento(EventoSistema evento) {
        if (this.rutaLog != null && !this.rutaLog.isEmpty()) {
            String linea = "[AUDITORIA] " + Instant.now() + " | "
                    + evento.getNombreEvento() + " | evento-ts: " + evento.getTimestamp();
            try (PrintWriter pw = new PrintWriter(new FileWriter(this.rutaLog, true))) {
                pw.println(linea);
            } catch (IOException ignored) {
            }
        }
    }
}
