package ec.edu.uce.certificadorforense.infrastructure.adapters.security;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.text.DateFormat;
import java.util.Date;

/**
 * jose4j's NumericDateValidator formatea fechas con java.text.DateFormat.getDateTimeInstance()
 * al validar cada JWT (io.quarkus.smallrye.jwt). La primera invocación de esa API en la vida
 * de la JVM inicializa en frío los LocaleServiceProvider de la plataforma y puede tardar varios
 * segundos, bloqueando el hilo del event-loop de Vert.x que atiende la primera petición autenticada
 * ("Thread ... has been blocked ... jose4j.jwt.NumericDate.toString"). Se fuerza aquí, en el arranque
 * (hilo principal, no el event-loop), para que ese costo no lo pague la primera request real.
 */
@ApplicationScoped
public class JwtDateFormatWarmup {

    private static final Logger LOG = Logger.getLogger(JwtDateFormatWarmup.class);

    void onStart(@Observes StartupEvent event) {
        long inicio = System.currentTimeMillis();
        DateFormat.getDateTimeInstance().format(new Date());
        LOG.infof("Warm-up de DateFormat (validación JWT) completado en %d ms", System.currentTimeMillis() - inicio);
    }
}
