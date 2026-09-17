package ec.edu.uce.certificadorforense.application.usecase;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.ports.in.RegistrarDatosObraUseCase;
import ec.edu.uce.certificadorforense.core.ports.out.EncryptionPort;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RegistrarDatosObraUseCaseImpl implements RegistrarDatosObraUseCase {

    @Inject
    ExpedienteRepositoryPort expedienteRepository;

    @Inject
    EncryptionPort encryptionPort;

    private static boolean esDuplicadoBloqueante(ExpedienteResumen exp) {
        return exp != null && ("CERTIFICADO".equals(exp.getEstadoActual()) || "FINALIZADO".equals(exp.getEstadoActual()));
    }

    @Override
    @Transactional
    public void ejecutar(String idExpediente, UsuarioDatos usuarioDb, Map<String, Object> body) {
        ExpedienteResumen expDb = expedienteRepository.buscarResumenPorId(idExpediente)
                .orElseThrow(() -> new RuntimeException("Expediente no encontrado o no ha sido analizado en Fase 1."));

        String nombresDec = usuarioDb.getNombres() != null ? encryptionPort.decrypt(usuarioDb.getNombres()) : "";
        String apellidosDec = usuarioDb.getApellidos() != null ? encryptionPort.decrypt(usuarioDb.getApellidos()) : "";
        String cedulaDec = usuarioDb.getCedula() != null ? encryptionPort.decrypt(usuarioDb.getCedula()) : "";

        Autor autor = Autor.builder()
                .nombres(nombresDec)
                .apellidos(apellidosDec)
                .cedula(cedulaDec)
                .correo(usuarioDb.getCorreo())
                .seudonimo(usuarioDb.getNombreArtistico() != null ? usuarioDb.getNombreArtistico() : "")
                .build();

        String categoriaStr = (String) body.get("categoria");
        CategoriaObra cat = CategoriaObra.ILUSTRACION; // Default
        for (CategoriaObra c : CategoriaObra.values()) {
            if (c.name().equalsIgnoreCase(categoriaStr)) {
                cat = c;
            }
        }

        String fechaCreacionStr = (String) body.get("fecha_creacion");
        LocalDate fechaCreacion = fechaCreacionStr != null && !fechaCreacionStr.isEmpty()
                ? LocalDate.parse(fechaCreacionStr)
                : LocalDate.now();

        Obra obra = Obra.builder()
                .titulo((String) body.get("titulo_obra"))
                .descripcion((String) body.get("descripcion"))
                .software((String) body.get("software"))
                .hardware((String) body.get("hardware"))
                .categoria(cat)
                .fechaCreacion(fechaCreacion)
                .build();

        Declaraciones decl = Declaraciones.builder()
                .titularDerechos((Boolean) body.getOrDefault("declaracion_derechos", false))
                .aceptaTerminos((Boolean) body.getOrDefault("declaracion_terminos", false))
                .build();

        Optional<ExpedienteResumen> expAnteriorOpt = expedienteRepository.buscarPorHashPsd(expDb.getHashPsdOriginal());
        if (expAnteriorOpt.isPresent()) {
            ExpedienteResumen expAnterior = expAnteriorOpt.get();
            if (!expAnterior.getIdExpediente().equals(expDb.getIdExpediente()) && esDuplicadoBloqueante(expAnterior)) {
                String mensajeError;
                if (expAnterior.getUsuarioId() != null && expAnterior.getUsuarioId().equals(usuarioDb.getId())) {
                    mensajeError = "El archivo PSD original ya se encuentra certificado en el sistema. No puedes certificar la misma obra dos veces.";
                } else {
                    mensajeError = "ALERTA DE SEGURIDAD: Esta obra ya se encuentra certificada y pertenece a otro autor.";
                    expedienteRepository.registrarAlertaSeguridad(expAnterior.getObraId(), usuarioDb.getCedula(),
                            usuarioDb.getNombres(), usuarioDb.getApellidos());
                }
                throw new RuntimeException(mensajeError);
            }
        }

        expedienteRepository.actualizarObraYDeclaraciones(idExpediente, usuarioDb.getId(), obra, cat, decl);
    }
}
