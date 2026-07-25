package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import ec.edu.uce.certificadorforense.core.observer.EventPublisher;
import ec.edu.uce.certificadorforense.core.observer.eventos.*;
import ec.edu.uce.certificadorforense.core.ports.out.*;
import ec.edu.uce.certificadorforense.core.service.*;
import ec.edu.uce.certificadorforense.core.state.*;
import ec.edu.uce.certificadorforense.core.rules.imagen.*;
import ec.edu.uce.certificadorforense.core.rules.psd.*;
import ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia.EsteganografiaPNGAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia.EsteganografiaJPEGAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorP12Adapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorPDFAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.hash.SHA512Adapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.pdf.GeneradorPDFAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.*;
import ec.edu.uce.certificadorforense.infrastructure.adapters.qr.QRGeneratorAdapter;
import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class CertificacionOrchestrator {

    @jakarta.inject.Inject
    HistorialHelperService historialHelper;

    @jakarta.inject.Inject
    ec.edu.uce.certificadorforense.infrastructure.adapters.output.IdentitySecurityAdapter identitySecurityAdapter;

    @jakarta.inject.Inject
    ec.edu.uce.certificadorforense.infrastructure.adapters.output.VaultSealAdapter vaultSealAdapter;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "tesis.cert.password")
    String certPassword;

    // Caché en memoria para las peticiones stateless del Frontend
    private final Map<String, ContextoProceso> contextoCache = new ConcurrentHashMap<>();

    private static class PsdCacheEntry {
        public ArchivoPSD psd;
        public String pHash;

        public PsdCacheEntry(ArchivoPSD psd, String pHash) {
            this.psd = psd;
            this.pHash = pHash;
        }
    }

    private final Map<String, PsdCacheEntry> cachePsdAnalysis = new ConcurrentHashMap<>();

    public static class JobStatus {
        public String estado;
        public String mensajeError;

        public JobStatus(String estado, String mensajeError) {
            this.estado = estado;
            this.mensajeError = mensajeError;
        }
    }

    private final Map<String, JobStatus> jobStatusCache = new ConcurrentHashMap<>();

    public JobStatus getJobStatus(String idExpediente) {
        return jobStatusCache.get(idExpediente);
    }

    public Map<String, String> iniciarAnalisisFase1(File psdFile, File imgFile, String extension) throws Exception {
        GeneradorHashPort hashPort = new SHA512Adapter();

        List<ArchivoProcessorPort<? extends ArchivoBase>> procesadores = Arrays.asList(
                new ArchivoImagenProcessor(),
                new ArchivoPSDProcessor());
        ArchivoProcessorFactory factory = new ArchivoProcessorFactory(procesadores);

        ValidadorGenericoService<ArchivoImagen> validadorImagen = new ValidadorGenericoService<>();
        validadorImagen.registrarRegla(new ReglaFirmaEstructural());
        validadorImagen.registrarRegla(new ReglaCoherenciaDpi());
        validadorImagen.registrarRegla(new ReglaAnalisisOrigen());

        ValidadorGenericoService<ArchivoPSD> validadorPSD = new ValidadorGenericoService<>();
        validadorPSD.registrarRegla(new ReglaFormatoPsd());
        validadorPSD.registrarRegla(new ReglaResolucionProfesional());
        validadorPSD.registrarRegla(new ReglaImagenPegada());
        validadorPSD.registrarRegla(new ReglaComplejidadDiseno());

        CalculadorPHash calcPHash = new CalculadorPHash();

        ContextoProceso contexto = new ContextoProceso(new AnalisisForenseState());

        String expedienteId = UUID.randomUUID().toString();

        try {
            // 1. Calcular Hash de la Imagen y procesarla primero (Early Return)
            String sha512Imagen = hashPort.calcularSHA512(imgFile);
            contexto.setSha512Imagen(sha512Imagen);

            ArchivoImagen imagen = ((ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(imgFile))
                    .procesar(imgFile);
            contexto.setArchivoImagen(imagen);
            contexto.setImagenRaw(Files.readAllBytes(imgFile.toPath())); // Guardar en RAM para evitar que /tmp lo borre

            VeredictoFinal veredictoImagen = validadorImagen.validar(imagen);
            if (veredictoImagen.isEsRechazado()) {
                throw new RuntimeException("Rechazo Imagen: " + veredictoImagen.getRazonRechazo());
            }
            contexto.setVeredictoImagen(veredictoImagen);

            // Calcular pHash de la imagen
            BufferedImage imgImagen = ImageLoader.loadWithSubsampling(imgFile);
            String pHashStr = calcPHash.generarHash(imgImagen);
            contexto.setPHash(pHashStr);

            // Verificar Duplicados tempranamente por el hash exacto de la Imagen
            ExpedienteForenseEntity expAnterior = ExpedienteForenseEntity
                    .find("hashImagenFinal", contexto.getSha512Imagen()).firstResult();

            if (expAnterior == null) {
                // Verificar por similitud visual (pHash cruzando formatos)
                java.util.List<ExpedienteForenseEntity> expedientesDB = ExpedienteForenseEntity
                        .list("phashImagenString is not null and obra.estadoActual in ('CERTIFICADO', 'FINALIZADO')");
                for (ExpedienteForenseEntity dbExp : expedientesDB) {
                    double pHashSimilitud = calcPHash.compararSimilitud(pHashStr, dbExp.phashImagenString);
                    if (pHashSimilitud >= 95.0) {
                        try {
                            com.google.gson.JsonObject jsonEvidencia = com.google.gson.JsonParser
                                    .parseString(dbExp.evidenciaTecnicaJson).getAsJsonObject();
                            if (jsonEvidencia.has("ancho")) {
                                int dbAncho = jsonEvidencia.get("ancho").getAsInt();
                                int dbAlto = jsonEvidencia.get("alto").getAsInt();
                                String dbExt = jsonEvidencia.has("extensionReal")
                                        ? jsonEvidencia.get("extensionReal").getAsString()
                                        : "";

                                int currentAncho = imagen.getMetadatos().getAncho();
                                int currentAlto = imagen.getMetadatos().getAlto();
                                String currentExt = imagen.getMetadatos().getExtensionReal();

                                if (dbAncho != currentAncho || dbAlto != currentAlto
                                        || !dbExt.equalsIgnoreCase(currentExt)) {
                                    throw new RuntimeException(
                                            "Forense: No certificamos variantes por políticas de integridad y conservación de pruebas.");
                                }
                            }
                        } catch (Exception e) {
                            if (e instanceof RuntimeException && e.getMessage().startsWith("Forense:")) {
                                throw e;
                            }
                            System.err.println("No se pudo parsear el JSON de evidencia para validar duplicado visual: "
                                    + e.getMessage());
                        }

                        expAnterior = dbExp;
                        break;
                    }
                }
            }

            // Si la IMAGEN es un duplicado exacto o visual idéntico, saltamos el PSD por
            // completo
            if (expAnterior != null && ("CERTIFICADO".equals(expAnterior.obra.estadoActual)
                    || "FINALIZADO".equals(expAnterior.obra.estadoActual))) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.hashImagenFinal); // Usamos el de la DB para recuperar el
                                                                              // ZIP original
                return resultado;
            }

            // 2. Procesar PSD utilizando caché
            String sha512PSD = hashPort.calcularSHA512(psdFile);
            contexto.setSha512PSD(sha512PSD);

            ArchivoPSD psd;
            String psdPHashStr;

            PsdCacheEntry entry = cachePsdAnalysis.get(sha512PSD);
            if (entry != null) {
                System.out.println("[Orchestrator] Usando análisis PSD desde caché para hash: " + sha512PSD);
                psd = entry.psd;
                psdPHashStr = entry.pHash;
            } else {
                psd = ((ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(psdFile)).procesar(psdFile);
                BufferedImage imgPSD = ImageLoader.loadWithSubsampling(psdFile);
                psdPHashStr = calcPHash.generarHash(imgPSD);
                cachePsdAnalysis.put(sha512PSD, new PsdCacheEntry(psd, psdPHashStr));
            }

            contexto.setArchivoPSD(psd);

            VeredictoFinal veredictoPSD = validadorPSD.validar(psd);
            if (veredictoPSD.isEsRechazado()) {
                throw new RuntimeException("Rechazo PSD: " + veredictoPSD.getRazonRechazo());
            }
            contexto.setVeredictoPSD(veredictoPSD);

            // Poblar campos del contexto que antes se manejaban con Listeners
            contexto.setCapasPSD(psd.getCapas() != null ? psd.getCapas().size() : 0);
            contexto.setMetadatosDetectados(psd.getMetadatos() != null);

            // 3. Comparar pHash (Similitud Visual)
            double similitud = calcPHash.compararSimilitud(psdPHashStr, pHashStr);
            // Umbral a 90% para compensar las ligeras diferencias generadas por compresión
            if (similitud < 90.0) {
                throw new RuntimeException("Rechazo Imagen: La similitud visual pHash no es suficiente ("
                        + String.format("%.2f", similitud) + "%). El PSD y la imagen no coinciden visualmente.");
            }

            // Verificar Duplicados por el hash del PSD (si no se encontró por imagen)
            if (expAnterior == null) {
                expAnterior = ExpedienteForenseEntity.find("hashPsdOriginal", contexto.getSha512PSD()).firstResult();
            }

            if (expAnterior != null && ("CERTIFICADO".equals(expAnterior.obra.estadoActual)
                    || "FINALIZADO".equals(expAnterior.obra.estadoActual))) {
                // Hay un duplicado exacto o visual idéntico. Devolvemos un estado especial para
                // pedir la cédula en el frontend.
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.hashImagenFinal); // Usamos el de la DB para recuperar el
                                                                              // ZIP original
                return resultado;
            }

            contexto.getEstadoActual().avanzar(contexto);

            contextoCache.put(expedienteId, contexto);

            Map<String, String> resultado = new HashMap<>();
            resultado.put("expediente_id", expedienteId);

            // Buscar software en PSD o Imagen
            if (psd.getMetadatos() != null && psd.getMetadatos().getSoftware() != null) {
                resultado.put("software_detectado", psd.getMetadatos().getSoftware());
            } else if (imagen.getMetadatos() != null && imagen.getMetadatos().getSoftware() != null) {
                resultado.put("software_detectado", imagen.getMetadatos().getSoftware());
            }

            return resultado;
        } finally {
            // Borrar archivos temporales de fase 1
            try {
                Files.deleteIfExists(psdFile.toPath());
            } catch (Exception ignored) {
            }
            try {
                Files.deleteIfExists(imgFile.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public void registrarDatosFase2(String idExpediente, UsuarioEntity usuarioDb, Map<String, Object> body) {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null)
            throw new RuntimeException("Expediente expirado o no existe.");

        Autor autor = Autor.builder()
                .nombres(usuarioDb.nombres)
                .apellidos(usuarioDb.apellidos)
                .cedula(usuarioDb.cedula)
                .correo(usuarioDb.correo)
                .seudonimo(usuarioDb.nombreArtistico != null ? usuarioDb.nombreArtistico : "")
                .build();

        String categoriaStr = (String) body.get("categoria");
        CategoriaObra cat = CategoriaObra.ILUSTRACION; // Default
        for (CategoriaObra c : CategoriaObra.values()) {
            if (c.name().equalsIgnoreCase(categoriaStr))
                cat = c;
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

        contexto.setAutor(autor);
        contexto.setObra(obra);
        contexto.setDeclaraciones(decl);

        // Solo avanzamos el estado de la máquina si venimos de Análisis Forense
        if (contexto.getEstadoActual() instanceof ec.edu.uce.certificadorforense.core.state.DatosObraState) {
            contexto.getEstadoActual().avanzar(contexto); // Avanza a FirmaAutorState
        }

        // PERSISTENCIA EN DB (Soporte para rectificación)
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));

        if (expDb == null) {
            // Verificar si el archivo ya fue subido en otro intento (hash duplicado)
            ExpedienteForenseEntity expAnterior = ExpedienteForenseEntity
                    .find("hashPsdOriginal", contexto.getSha512PSD()).firstResult();

            if (expAnterior != null) {
                if ("CERTIFICADO".equals(expAnterior.obra.estadoActual)
                        || "FINALIZADO".equals(expAnterior.obra.estadoActual)) {
                    String mensajeError;
                    if (expAnterior.obra.usuario.id.equals(usuarioDb.id)) {
                        mensajeError = "El archivo PSD original ya se encuentra certificado en el sistema. No puedes certificar la misma obra dos veces.";
                    } else {
                        mensajeError = "ALERTA DE SEGURIDAD: Esta obra ya se encuentra certificada y pertenece a otro autor.";
                        // Registrar en el historial en una transacción independiente para que no se
                        // deshaga al lanzar la excepción
                        historialHelper.registrarAlerta(expAnterior.obra, usuarioDb.cedula, usuarioDb.nombres,
                                usuarioDb.apellidos);
                    }
                    throw new RuntimeException(mensajeError);
                } else {
                    // Es un borrador huérfano (el usuario regresó al Paso 1 y subió el mismo
                    // archivo)
                    // Eliminamos el borrador anterior para permitir este nuevo intento.
                    HistorialEstadoEntity.delete("obra", expAnterior.obra);
                    DeclaracionesObraEntity.delete("obra", expAnterior.obra);
                    FirmaAutorEntity.delete("expediente", expAnterior);
                    expAnterior.delete();
                    expAnterior.obra.delete();

                    // Forzar el flush para que los DELETEs se ejecuten en SQL ANTES del INSERT
                    ExpedienteForenseEntity.getEntityManager().flush();
                }
            }

            ObraEntity obraEntity = new ObraEntity();
            obraEntity.id = UUID.randomUUID();
            obraEntity.usuario = usuarioDb;
            obraEntity.titulo = obra.getTitulo();
            obraEntity.descripcion = obra.getDescripcion();
            obraEntity.categoria = cat.name();
            obraEntity.software = obra.getSoftware();
            obraEntity.hardware = obra.getHardware();
            obraEntity.fechaCreacion = obra.getFechaCreacion();
            obraEntity.fechaRegistro = LocalDateTime.now();
            obraEntity.estadoActual = "ESPERANDO_FIRMA";
            obraEntity.persist();

            DeclaracionesObraEntity decDb = new DeclaracionesObraEntity();
            decDb.id = UUID.randomUUID();
            decDb.obra = obraEntity;
            decDb.esTitularDerechos = decl.isTitularDerechos();
            decDb.aceptaTerminosCertificacion = decl.isAceptaTerminos();
            decDb.fechaAceptacion = LocalDateTime.now();
            decDb.ipRegistro = (String) body.get("ip_registro");
            decDb.persist();

            expDb = new ExpedienteForenseEntity();
            expDb.id = UUID.fromString(idExpediente);
            expDb.obra = obraEntity;
            expDb.hashPsdOriginal = contexto.getSha512PSD();
            expDb.hashImagenFinal = contexto.getSha512Imagen();
            expDb.phashImagenString = contexto.getPHash();
            expDb.similitudPhash = new java.math.BigDecimal("99.99");
            expDb.resultadoAnalisis = "APROBADO";

            com.google.gson.JsonObject jsonEv = new com.google.gson.JsonObject();
            if (contexto.getArchivoImagen() != null && contexto.getArchivoImagen().getMetadatos() != null) {
                jsonEv.addProperty("ancho", contexto.getArchivoImagen().getMetadatos().getAncho());
                jsonEv.addProperty("alto", contexto.getArchivoImagen().getMetadatos().getAlto());
                jsonEv.addProperty("extensionReal", contexto.getArchivoImagen().getMetadatos().getExtensionReal());
            }
            expDb.evidenciaTecnicaJson = jsonEv.toString();

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
        } else {
            // Rectificación: Actualizar obra existente
            ObraEntity obraEntity = expDb.obra;
            obraEntity.usuario = usuarioDb;
            obraEntity.titulo = obra.getTitulo();
            obraEntity.descripcion = obra.getDescripcion();
            obraEntity.categoria = cat.name();
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
            decDb.esTitularDerechos = decl.isTitularDerechos();
            decDb.aceptaTerminosCertificacion = decl.isAceptaTerminos();
            decDb.fechaAceptacion = LocalDateTime.now();
            decDb.ipRegistro = (String) body.get("ip_registro");
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
    }

    @Transactional
    public String firmarFase3(String idExpediente, String password) throws Exception {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null)
            throw new RuntimeException("Expediente expirado o no existe.");

        ExpedienteService expedienteServ = new ExpedienteService();
        Expediente expediente = expedienteServ.construir(contexto);
        contexto.setExpediente(expediente);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (com.google.gson.JsonSerializer<LocalDate>) (src, typeOfSrc,
                                context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc,
                                context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .setPrettyPrinting()
                .create();
        String expedienteJson = gson.toJson(expediente);
        contexto.setExpedienteJson(expedienteJson);

        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        if (expDb == null)
            throw new RuntimeException("Expediente no encontrado en BD.");

        UsuarioEntity usuario = expDb.obra.usuario;
        String p12Base64 = usuario.firmaP12;
        if (p12Base64 == null || p12Base64.isEmpty()) {
            throw new RuntimeException("El usuario no tiene una firma digital configurada en el sistema.");
        }

        // Calcular Hash del Expediente
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(expedienteJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashObra = sb.toString();

        String firmaBase64;
        try {
            firmaBase64 = identitySecurityAdapter.getAuthorDigitalSignature(p12Base64, password, hashObra);
        } catch (jakarta.ws.rs.WebApplicationException we) {
            we.printStackTrace();
            String responseBody = we.getResponse().readEntity(String.class);
            throw new RuntimeException("Respuesta de la CA: " + responseBody);
        } catch (Exception e) {
            // Imprimir el error REAL en la consola de Quarkus para depurar si es fallo de
            // Azure, de payload o timeout
            e.printStackTrace();
            // Retornamos el error original para que lo puedas ver en la pantalla de Vue!
            String errorReal = e.getMessage() != null ? e.getMessage() : e.toString();
            throw new RuntimeException("Fallo de conexión con Azure: " + errorReal);
        }

        FirmaAutor firma = FirmaAutor.builder()
                .firmaBase64(firmaBase64)
                .hashExpediente(hashObra)
                .algoritmo("SHA512withRSA")
                .fechaFirma(java.time.Instant.now())
                .aliasKeystore("AzureCloud")
                .build();
        contexto.setFirmaAutor(firma);

        if (contexto.getEstadoActual() instanceof ec.edu.uce.certificadorforense.core.state.FirmaAutorState) {
            contexto.getEstadoActual().avanzar(contexto); // Avanza a CertificadoState
        }

        // PERSISTENCIA DB
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

        // Actualizar obra
        expDb.obra.estadoActual = "CERTIFICADO";
        expDb.obra.persist();

        // **NUEVO FLUJO**: Generar el ZIP de forma asíncrona pero antes de responder
        // para que esté listo cuando el usuario haga clic en descargar.
        byte[] zipGenerado = emitirCertificadoFase4(idExpediente);
        contexto.setZipGenerado(zipGenerado);

        return firma.getHashExpediente();
    }

    @Transactional
    public byte[] emitirCertificadoFase4(String idExpediente) throws Exception {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null)
            throw new RuntimeException("Expediente expirado o no existe.");

        GeneradorQRPort qrPort = new QRGeneratorAdapter();
        GeneradorHashPort hashPort = new SHA512Adapter();
        CertificadoService certServ = new CertificadoService(qrPort, hashPort);
        GeneradorPDFPort generadorPDF = new GeneradorPDFAdapter();

        String expedienteFirmadoJson = contexto.getExpedienteJson() + "\n---FIRMA---\n"
                + contexto.getFirmaAutor().getFirmaBase64();
        Certificado certificado = certServ.generar(contexto.getExpediente(), expedienteFirmadoJson);
        contexto.setCertificado(certificado);

        String imagenBase64 = Base64.getEncoder().encodeToString(contexto.getImagenRaw());

        byte[] pdfSinFirmar = generadorPDF.generar(certificado, contexto.getExpediente(), expedienteFirmadoJson,
                imagenBase64);

        byte[] pdfFirmado = pdfSinFirmar;
        try {
            pdfFirmado = vaultSealAdapter.applyInstitutionalSeal(pdfSinFirmar, certPassword);
        } catch (Exception e) {
            System.err.println(
                    "Fallback: No se pudo firmar el PDF con Azure Key Vault. Enviando PDF sin firmar. Detalle: "
                            + e.getMessage());
        }
        contexto.setPdfCertificado(pdfFirmado);

        // Esteganografia
        String rutaImagen = contexto.getArchivoImagen().getRutaAbsoluta().toLowerCase();
        EsteganografiaPort estegano;
        if (rutaImagen.endsWith(".jpg") || rutaImagen.endsWith(".jpeg")) {
            estegano = new EsteganografiaJPEGAdapter();
        } else {
            estegano = new EsteganografiaPNGAdapter();
        }

        String jsonEstegano = "{\"id\":\"" + certificado.getIdCertificado() + "\",\"hash\":\""
                + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = estegano.inyectar(contexto.getImagenRaw(), jsonEstegano);
        contexto.setImagenCertificada(imagenCert);

        // Persistencia Final DB
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

        // Generar Zip
        java.io.ByteArrayOutputStream baosZip = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baosZip);

        java.util.zip.ZipEntry pdfEntry = new java.util.zip.ZipEntry(certificado.getIdCertificado() + ".pdf");
        zos.putNextEntry(pdfEntry);
        zos.write(pdfFirmado);
        zos.closeEntry();

        String imgExt = rutaImagen.endsWith(".jpg") || rutaImagen.endsWith(".jpeg") ? ".jpg" : ".png";
        java.util.zip.ZipEntry imgEntry = new java.util.zip.ZipEntry(
                certificado.getIdCertificado() + "-obra-certificada" + imgExt);
        zos.putNextEntry(imgEntry);
        zos.write(imagenCert);
        zos.closeEntry();
        zos.close();

        return baosZip.toByteArray();
    }

    public byte[] obtenerZipYLimpiar(String idExpediente) {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null) {
            throw new RuntimeException("Expediente expirado o no existe.");
        }
        byte[] zip = contexto.getZipGenerado();
        if (zip == null) {
            throw new RuntimeException("El archivo ZIP no se generó correctamente en el paso anterior.");
        }

        // Limpiar memoria AHORA (en el paso 4) para evitar saturación de RAM
        contextoCache.remove(idExpediente);
        return zip;
    }
}
