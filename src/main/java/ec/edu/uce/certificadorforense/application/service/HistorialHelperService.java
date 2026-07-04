package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.HistorialEstadoEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.ObraEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class HistorialHelperService {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrarAlerta(ObraEntity obra, String cedula, String nombres, String apellidos) {
        HistorialEstadoEntity histAlerta = new HistorialEstadoEntity();
        histAlerta.id = UUID.randomUUID();
        histAlerta.obra = obra;
        histAlerta.estadoAnterior = obra.estadoActual;
        histAlerta.estadoNuevo = obra.estadoActual; // No cambia de estado
        histAlerta.fechaCambio = LocalDateTime.now();
        histAlerta.observacion = "ALERTA: Intento de registro duplicado/plagio detectado. Usuario que intentó registrar: " 
                                + cedula + " - " + nombres + " " + apellidos;
        histAlerta.persist();
    }
}
