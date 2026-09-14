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

import ec.edu.uce.certificadorforense.infrastructure.adapters.security.VaultEncryptionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
@Transactional
public class ExpedienteJpaRepositoryAdapter implements ExpedienteRepositoryPort {

    @Inject
    VaultEncryptionService vaultEncryptionService;

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
    public void crearBorradorAnalisis(String idExpediente, UUID usuarioId, String hashPsd, String hashImagen,
                                       String phash, String evidenciaTecnicaJson, byte[] imagenRaw) {
        UsuarioEntity usuario = usuarioId != null ? UsuarioEntity.findById(usuarioId) : null;
        if (usuario == null) {
            usuario = UsuarioEntity.find("activo", true).firstResult();
            if (usuario == null) {
                usuario = UsuarioEntity.findAll().firstResult();
            }
        }

        ObraEntity obraEntity = new ObraEntity();
        obraEntity.id = UUID.randomUUID();
        obraEntity.usuario = usuario;
        obraEntity.titulo = "Borrador de análisis forense";
        obraEntity.fechaRegistro = LocalDateTime.now();
        obraEntity.estadoActual = "ANALIZADO";
        obraEntity.persist();

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
        expDb.imagenRaw = imagenRaw;
        expDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = obraEntity;
        hist.estadoAnterior = "INICIADO";
        hist.estadoNuevo = "ANALIZADO";
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Fase 1 completada. Análisis forense aprobado y registrado como borrador.";
        hist.persist();
    }

    @Override
    public byte[] obtenerImagenRaw(String idExpediente) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        return expDb != null ? expDb.imagenRaw : null;
    }

    @Override
    public void limpiarImagenRaw(String idExpediente) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        if (expDb != null) {
            expDb.imagenRaw = null;
            expDb.persist();
        }
    }

    @Override
    public Optional<ExpedienteResumen> buscarResumenPorId(String idExpediente) {
        ExpedienteForenseEntity exp = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        return Optional.ofNullable(exp).map(this::toResumen);
    }

    private ExpedienteResumen toResumen(ExpedienteForenseEntity exp) {
        ObraEntity obra = exp.obra;
        UsuarioEntity usuario = obra != null ? obra.usuario : null;
        return ExpedienteResumen.builder()
                .idExpediente(exp.id)
                .obraId(obra != null ? obra.id : null)
                .estadoActual(obra != null ? obra.estadoActual : null)
                .hashPsdOriginal(exp.hashPsdOriginal)
                .hashImagenFinal(exp.hashImagenFinal)
                .phashImagenString(exp.phashImagenString)
                .evidenciaTecnicaJson(exp.evidenciaTecnicaJson)
                .usuarioId(usuario != null ? usuario.id : null)
                .usuarioCedula(usuario != null && usuario.cedula != null ? vaultEncryptionService.decrypt(usuario.cedula) : null)
                .usuarioFirmaP12(usuario != null ? usuario.firmaP12 : null)
                .usuarioNombres(usuario != null && usuario.nombres != null ? vaultEncryptionService.decrypt(usuario.nombres) : null)
                .usuarioApellidos(usuario != null && usuario.apellidos != null ? vaultEncryptionService.decrypt(usuario.apellidos) : null)
                .usuarioCorreo(usuario != null ? usuario.correo : null)
                .usuarioNombreArtistico(usuario != null ? usuario.nombreArtistico : null)
                .obraTitulo(obra != null ? obra.titulo : null)
                .obraDescripcion(obra != null ? obra.descripcion : null)
                .obraCategoria(obra != null ? obra.categoria : null)
                .obraSoftware(obra != null ? obra.software : null)
                .obraHardware(obra != null ? obra.hardware : null)
                .obraFechaCreacion(obra != null ? obra.fechaCreacion : null)
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
                                               Declaraciones declaraciones,
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
                                              Declaraciones declaraciones) {
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
        decDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = obraEntity;
        hist.estadoAnterior = obraEntity.estadoActual;
        hist.estadoNuevo = "ESPERANDO_FIRMA";
        hist.fechaCambio = LocalDateTime.now();
        hist.observacion = "Fase 2 completada. Datos de obra y declaraciones registradas.";
        hist.persist();

        obraEntity.estadoActual = "ESPERANDO_FIRMA";
        obraEntity.persist();
    }

    @Override
    public void guardarFirma(String idExpediente, FirmaAutor firma) {
        guardarFirma(idExpediente, firma, null);
    }

    @Override
    public void guardarFirma(String idExpediente, FirmaAutor firma, String expedienteJson) {
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
        if (expedienteJson != null) {
            firmaDb.expedienteJson = expedienteJson;
        }
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
    public Optional<FirmaAutor> buscarFirmaPorExpedienteId(String idExpediente) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        if (expDb == null) return Optional.empty();
        FirmaAutorEntity firmaDb = FirmaAutorEntity.find("expediente", expDb).firstResult();
        if (firmaDb == null) return Optional.empty();
        return Optional.of(FirmaAutor.builder()
                .idFirma(firmaDb.id.toString())
                .idExpediente(idExpediente)
                .hashExpediente(firmaDb.hashFirmado)
                .firmaBase64(firmaDb.firmaBase64)
                .algoritmo(firmaDb.algoritmo)
                .expedienteJson(firmaDb.expedienteJson)
                .build());
    }

    @Override
    public void guardarCertificado(String idExpediente, Certificado certificado, String expedienteFirmadoJson) {
        guardarCertificado(idExpediente, certificado, expedienteFirmadoJson, null);
    }

    @Override
    public void guardarCertificado(String idExpediente, Certificado certificado, String expedienteFirmadoJson, byte[] paqueteZip) {
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
        if (paqueteZip != null) {
            certDb.paqueteZip = paqueteZip;
        }
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
    public byte[] obtenerZipCertificado(String idExpediente) {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        if (expDb == null || expDb.obra == null) return null;
        CertificadoEntity certDb = CertificadoEntity.find("obra", expDb.obra).firstResult();
        return certDb != null ? certDb.paqueteZip : null;
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
                .usuarioCedula(vaultEncryptionService.decrypt(expDb.obra.usuario.cedula))
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
