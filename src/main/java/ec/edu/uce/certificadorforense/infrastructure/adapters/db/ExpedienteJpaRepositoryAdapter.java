package ec.edu.uce.certificadorforense.infrastructure.adapters.db;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.RecuperacionDatos;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de infraestructura: toda la persistencia relacional del
 * expediente forense (obra, declaraciones, firma, certificado, historial).
 * Es la única implementación de {@link ExpedienteRepositoryPort} — antes esta
 * lógica vivía inline en {@code CertificacionOrchestrator}, acoplando la
 * capa de aplicación a las entidades JPA.
 */
@ApplicationScoped
public class ExpedienteJpaRepositoryAdapter implements ExpedienteRepositoryPort {

    @Override
    public Optional<ExpedienteResumen> buscarPorHashImagen(String hashImagen) {
        ExpedienteForenseEntity exp = ExpedienteForenseEntity.find("hashImagenFinal", hashImagen).firstResult();
        return Optional.ofNullable(exp).map(this::toResumen);
    }

    @Override
    public Optional<ExpedienteResumen> buscarPorHashPsd(String hashPsd) {
        ExpedienteForenseEntity exp = ExpedienteForenseEntity.find("hashPsdOriginal", hashPsd).firstResult();
        return Optional.ofNullable(exp).map(this::toResumen);
    }

    @Override
    public List<ExpedienteResumen> listarCertificadosConPHash() {
        List<ExpedienteForenseEntity> expedientes = ExpedienteForenseEntity
                .list("phashImagenString is not null and obra.estadoActual in ('CERTIFICADO', 'FINALIZADO')");
        return expedientes.stream().map(this::toResumen).toList();
    }

    @Override
    public Optional<ExpedienteResumen> buscarResumenPorId(String idExpediente) {
        ExpedienteForenseEntity exp = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        return Optional.ofNullable(exp).map(this::toResumen);
    }

    private ExpedienteResumen toResumen(ExpedienteForenseEntity exp) {
        UsuarioEntity usuario = exp.obra.usuario;
        return ExpedienteResumen.builder()
                .idExpediente(exp.id)
                .obraId(exp.obra.id)
                .estadoActual(exp.obra.estadoActual)
                .hashPsdOriginal(exp.hashPsdOriginal)
                .hashImagenFinal(exp.hashImagenFinal)
                .phashImagenString(exp.phashImagenString)
                .evidenciaTecnicaJson(exp.evidenciaTecnicaJson)
                .usuarioId(usuario != null ? usuario.id : null)
                .usuarioCedula(usuario != null ? usuario.cedula : null)
                .usuarioFirmaP12(usuario != null ? usuario.firmaP12 : null)
                .build();
    }

    @Override
    public void eliminarBorrador(UUID obraId) {
        ObraEntity obra = ObraEntity.findById(obraId);
        if (obra == null) {
            return;
        }
        ExpedienteForenseEntity exp = ExpedienteForenseEntity.find("obra", obra).firstResult();

        // delete("campo", valor) es un DELETE HQL en bloque: no pasa por el ciclo de vida normal
        // de la entidad, así que Hibernate no se entera de que esas filas ya no existen si las
        // instancias siguen en la sesión (ej. recién persistidas en el mismo request/transacción)
        // — eso revienta con TransientPropertyValueException al hacer flush más adelante. Cargar
        // cada instancia y borrarla sí mantiene todo dentro del ciclo de vida que Hibernate rastrea.
        HistorialEstadoEntity.<HistorialEstadoEntity>list("obra", obra).forEach(HistorialEstadoEntity::delete);
        DeclaracionesObraEntity.<DeclaracionesObraEntity>list("obra", obra).forEach(DeclaracionesObraEntity::delete);
        if (exp != null) {
            FirmaAutorEntity.<FirmaAutorEntity>list("expediente", exp).forEach(FirmaAutorEntity::delete);
            exp.delete();
        }
        obra.delete();

        // Forzar el flush para que los DELETEs se ejecuten en SQL ANTES del INSERT que sigue.
        ObraEntity.getEntityManager().flush();
    }

    @Override
    public void registrarNuevaObraYExpediente(String idExpediente, UUID usuarioId, Obra obra, CategoriaObra categoria,
                                               Declaraciones declaraciones, String ipRegistro,
                                               String hashPsd, String hashImagen, String phash, String evidenciaTecnicaJson) {
        UsuarioEntity usuario = UsuarioEntity.findById(usuarioId);

        ObraEntity obraEntity = new ObraEntity();
        obraEntity.id = UUID.randomUUID();
        obraEntity.usuario = usuario;
        obraEntity.titulo = obra.getTitulo();
        obraEntity.descripcion = obra.getDescripcion();
        obraEntity.categoria = categoria.name();
        obraEntity.software = obra.getSoftware();
        obraEntity.hardware = obra.getHardware();
        obraEntity.fechaCreacion = obra.getFechaCreacion();
        obraEntity.fechaRegistro = LocalDateTime.now();
        obraEntity.estadoActual = "ESPERANDO_FIRMA";
        obraEntity.persist();

        DeclaracionesObraEntity decDb = new DeclaracionesObraEntity();
        decDb.id = UUID.randomUUID();
        decDb.obra = obraEntity;
        decDb.esTitularDerechos = declaraciones.isTitularDerechos();
        decDb.aceptaTerminosCertificacion = declaraciones.isAceptaTerminos();
        decDb.fechaAceptacion = LocalDateTime.now();
        decDb.ipRegistro = ipRegistro;
        decDb.persist();

        ExpedienteForenseEntity expDb = new ExpedienteForenseEntity();
        expDb.id = UUID.fromString(idExpediente);
        expDb.obra = obraEntity;
        expDb.hashPsdOriginal = hashPsd;
        expDb.hashImagenFinal = hashImagen;
        expDb.phashImagenString = phash;
        expDb.similitudPhash = new java.math.BigDecimal("99.99");
        expDb.resultadoAnalisis = "APROBADO";
        expDb.evidenciaTecnicaJson = evidenciaTecnicaJson;
        expDb.fechaAnalisis = LocalDateTime.now();
        expDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = obraEntity;
        hist.estadoAnterior = "ANALIZADO";
        hist.estadoNuevo = "ESPERANDO_FIRMA";
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Fase 2 completada.";
        hist.persist();
    }

    @Override
    public void actualizarObraYDeclaraciones(String idExpediente, UUID usuarioId, Obra obra, CategoriaObra categoria,
                                              Declaraciones declaraciones, String ipRegistro) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        UsuarioEntity usuario = UsuarioEntity.findById(usuarioId);

        ObraEntity obraEntity = expDb.obra;
        obraEntity.usuario = usuario;
        obraEntity.titulo = obra.getTitulo();
        obraEntity.descripcion = obra.getDescripcion();
        obraEntity.categoria = categoria.name();
        obraEntity.software = obra.getSoftware();
        obraEntity.hardware = obra.getHardware();
        obraEntity.fechaCreacion = obra.getFechaCreacion();
        obraEntity.persist();

        DeclaracionesObraEntity decDb = DeclaracionesObraEntity.find("obra", obraEntity).firstResult();
        if (decDb == null) {
            decDb = new DeclaracionesObraEntity();
            decDb.id = UUID.randomUUID();
            decDb.obra = obraEntity;
        }
        decDb.esTitularDerechos = declaraciones.isTitularDerechos();
        decDb.aceptaTerminosCertificacion = declaraciones.isAceptaTerminos();
        decDb.fechaAceptacion = LocalDateTime.now();
        decDb.ipRegistro = ipRegistro;
        decDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = obraEntity;
        hist.estadoAnterior = obraEntity.estadoActual;
        hist.estadoNuevo = obraEntity.estadoActual;
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Rectificación de datos de Fase 2.";
        hist.persist();
    }

    @Override
    public void guardarFirma(String idExpediente, FirmaAutor firma) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));

        FirmaAutorEntity firmaDb = FirmaAutorEntity.find("expediente", expDb).firstResult();
        if (firmaDb == null) {
            firmaDb = new FirmaAutorEntity();
            firmaDb.id = UUID.randomUUID();
            firmaDb.expediente = expDb;
        }
        firmaDb.usuario = expDb.obra.usuario;
        firmaDb.hashFirmado = firma.getHashExpediente();
        firmaDb.firmaBase64 = firma.getFirmaBase64();
        firmaDb.algoritmo = firma.getAlgoritmo();
        firmaDb.fechaFirma = LocalDateTime.now();
        firmaDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = expDb.obra;
        hist.estadoAnterior = expDb.obra.estadoActual;
        hist.estadoNuevo = "CERTIFICADO";
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Obra firmada digitalmente.";
        hist.persist();

        expDb.obra.estadoActual = "CERTIFICADO";
        expDb.obra.persist();
    }

    @Override
    public void guardarCertificado(String idExpediente, Certificado certificado, String expedienteFirmadoJson) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));

        CertificadoEntity certDb = CertificadoEntity.find("obra", expDb.obra).firstResult();
        if (certDb == null) {
            certDb = new CertificadoEntity();
            certDb.id = UUID.randomUUID();
            certDb.obra = expDb.obra;
        }
        certDb.numeroCertificado = certificado.getIdCertificado();
        certDb.expedienteFirmadoRaw = expedienteFirmadoJson;
        certDb.hashCertificado = certificado.getHashExpedienteFirmado();
        certDb.rutaPdfNube = "DB_BLOB";
        certDb.rutaPngNube = "DB_BLOB";
        certDb.fechaEmision = LocalDateTime.now();
        certDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = expDb.obra;
        hist.estadoAnterior = expDb.obra.estadoActual;
        hist.estadoNuevo = "FINALIZADO";
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Fase 4 completada. Certificado y ZIP generados.";
        hist.persist();

        expDb.obra.estadoActual = "FINALIZADO";
        expDb.obra.persist();
    }

    @Override
    public Optional<RecuperacionDatos> buscarParaRecuperacion(String hashImagenODePsd) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.find("hashImagenFinal", hashImagenODePsd).firstResult();
        if (expDb == null) {
            expDb = ExpedienteForenseEntity.find("hashPsdOriginal", hashImagenODePsd).firstResult();
        }
        if (expDb == null) {
            return Optional.empty();
        }

        CertificadoEntity certDb = CertificadoEntity.find("obra", expDb.obra).firstResult();
        if (certDb == null) {
            return Optional.empty();
        }

        Certificado certificado = Certificado.builder()
                .idCertificado(certDb.numeroCertificado)
                .idExpediente(expDb.id.toString())
                .fechaEmision(certDb.fechaEmision.toInstant(java.time.ZoneOffset.UTC))
                .hashExpedienteFirmado(certDb.hashCertificado)
                .qrContenido(certDb.numeroCertificado)
                .build();

        return Optional.of(RecuperacionDatos.builder()
                .usuarioCedula(expDb.obra.usuario.cedula)
                .certificado(certificado)
                .expedienteFirmadoRaw(certDb.expedienteFirmadoRaw)
                .build());
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrarAlertaSeguridad(UUID obraId, String cedulaIntento, String nombresIntento, String apellidosIntento) {
        ObraEntity obra = ObraEntity.findById(obraId);
        if (obra == null) {
            return;
        }
        HistorialEstadoEntity histAlerta = new HistorialEstadoEntity();
        histAlerta.id = UUID.randomUUID();
        histAlerta.obra = obra;
        histAlerta.estadoAnterior = obra.estadoActual;
        histAlerta.estadoNuevo = obra.estadoActual; // No cambia de estado
        histAlerta.fechaCambio = LocalDateTime.now();
        histAlerta.observacion = "ALERTA: Intento de registro duplicado/plagio detectado. Usuario que intentó registrar: "
                + cedulaIntento + " - " + nombresIntento + " " + apellidosIntento;
        histAlerta.persist();
    }
}
