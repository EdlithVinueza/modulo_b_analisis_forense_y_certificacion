package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import ec.edu.uce.certificadorforense.core.ports.out.*;
import ec.edu.uce.certificadorforense.core.service.*;
import ec.edu.uce.certificadorforense.core.state.stateinterface.*;
import ec.edu.uce.certificadorforense.core.state.stateimplement.*;
import ec.edu.uce.certificadorforense.core.rules.rulesinterface.*;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.*;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.*;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.*;
import ec.edu.uce.certificadorforense.core.model.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.infrastructure.adapters.security.VaultEncryptionService;

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

@ApplicationScoped
public class CertificacionOrchestrator {

    @jakarta.inject.Inject
    ExpedienteRepositoryPort expedienteRepository;

    /** Puerto de firma digital en la nube — implementado por IdentitySecurityAdapter. */
    @jakarta.inject.Inject
    FirmadorNubePort firmadorNube;

    /** Puerto de sellado institucional del PDF — implementado por VaultSealAdapter. */
    @jakarta.inject.Inject
    SelladorInstitucionalPort selladorInstitucional;

    @jakarta.inject.Inject
    VaultEncryptionService vaultEncryptionService;

    @jakarta.inject.Inject
    GeneradorHashPort hashPort;

    @jakarta.inject.Inject
    HashSHA512Service hashService;

    @jakarta.inject.Inject
    GeneradorQRPort qrPort;

    @jakarta.inject.Inject
    GeneradorPDFPort generadorPDF;

    /** Inyector de datos para JPEG — implementado por InyeccionDatosJPEGAdapter (@Named("jpeg")). */
    @jakarta.inject.Inject
    @jakarta.inject.Named("jpeg")
    InyeccionDatosPort inyeccionJpeg;

    /** Inyector de datos para PNG — implementado por InyeccionDatosPNGAdapter (@Named("png")). */
    @jakarta.inject.Inject
    @jakarta.inject.Named("png")
    InyeccionDatosPort inyeccionPng;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "tesis.cert.password")
    String certPassword;

    // Tiempo que una certificación puede quedar a medias antes de considerarse abandonada
    // (usuario cerró el navegador en medio del wizard) y liberar su memoria.
    private static final java.time.Duration TTL_CACHE = java.time.Duration.ofHours(2);

    // Caché en memoria para las peticiones stateless del Frontend
    private final Map<String, ContextoProceso> contextoCache = new ConcurrentHashMap<>();
    // Fecha de creación de cada entrada de contextoCache, para poder expirarla (ver limpiarCachesExpiradas).
    private final Map<String, java.time.Instant> contextoCreadoEn = new ConcurrentHashMap<>();

    private static class PsdCacheEntry {
        public ArchivoPSD psd;
        public String pHash;
        public final java.time.Instant creadoEn = java.time.Instant.now();

        public PsdCacheEntry(ArchivoPSD psd, String pHash) {
            this.psd = psd;
            this.pHash = pHash;
        }
    }

    private final Map<String, PsdCacheEntry> cachePsdAnalysis = new ConcurrentHashMap<>();

    // PSD y expedientes pueden pesar cientos de MB (ver quarkus.http.limits.max-body-size) — sin
    // esto, cachePsdAnalysis y contextoCache crecen sin límite mientras el proceso viva.
    @io.quarkus.scheduler.Scheduled(every = "30m")
    void limpiarCachesExpiradas() {
        java.time.Instant limite = java.time.Instant.now().minus(TTL_CACHE);

        contextoCreadoEn.entrySet().removeIf(entry -> {
            boolean expirado = entry.getValue().isBefore(limite);
            if (expirado) {
                contextoCache.remove(entry.getKey());
            }
            return expirado;
        });

        cachePsdAnalysis.entrySet().removeIf(entry -> entry.getValue().creadoEn.isBefore(limite));
    }

    private static boolean esDuplicadoBloqueante(ExpedienteResumen exp) {
        return exp != null && ("CERTIFICADO".equals(exp.getEstadoActual()) || "FINALIZADO".equals(exp.getEstadoActual()));
    }

    public Map<String, String> iniciarAnalisisFase1(File psdFile, File imgFile, String extension) throws Exception {
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
            String sha512Imagen = hashService.calcular(imgFile);
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
            ExpedienteResumen expAnterior = expedienteRepository.buscarPorHashImagen(contexto.getSha512Imagen())
                    .orElse(null);

            if (expAnterior == null) {
                // Verificar por similitud visual (pHash cruzando formatos)
                List<ExpedienteResumen> candidatos = expedienteRepository.listarCertificadosConPHash();
                for (ExpedienteResumen dbExp : candidatos) {
                    double pHashSimilitud = calcPHash.compararSimilitud(pHashStr, dbExp.getPhashImagenString());
                    if (pHashSimilitud >= 95.0) {
                        try {
                            com.google.gson.JsonObject jsonEvidencia = com.google.gson.JsonParser
                                    .parseString(dbExp.getEvidenciaTecnicaJson()).getAsJsonObject();
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
            if (esDuplicadoBloqueante(expAnterior)) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.getHashImagenFinal()); // Usamos el de la DB para recuperar el
                                                                                    // ZIP original
                return resultado;
            }

            // 2. Procesar PSD utilizando caché
            String sha512PSD = hashService.calcular(psdFile);
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
                expAnterior = expedienteRepository.buscarPorHashPsd(contexto.getSha512PSD()).orElse(null);
            }

            if (esDuplicadoBloqueante(expAnterior)) {
                // Hay un duplicado exacto o visual idéntico. Devolvemos un estado especial para
                // pedir la cédula en el frontend.
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.getHashImagenFinal()); // Usamos el de la DB para recuperar el
                                                                                    // ZIP original
                return resultado;
            }

            contexto.getEstadoActual().avanzar(contexto);

            contextoCache.put(expedienteId, contexto);
            contextoCreadoEn.put(expedienteId, java.time.Instant.now());

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
    public void registrarDatosFase2(String idExpediente, UsuarioDatos usuarioDb, Map<String, Object> body) {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null)
            throw new RuntimeException("Expediente expirado o no existe.");

        String nombresDec = usuarioDb.getNombres() != null ? vaultEncryptionService.decrypt(usuarioDb.getNombres()) : "";
        String apellidosDec = usuarioDb.getApellidos() != null ? vaultEncryptionService.decrypt(usuarioDb.getApellidos()) : "";

        Autor autor = Autor.builder()
                .nombres(nombresDec)
                .apellidos(apellidosDec)
                .cedula(usuarioDb.getCedula())
                .correo(usuarioDb.getCorreo())
                .seudonimo(usuarioDb.getNombreArtistico() != null ? usuarioDb.getNombreArtistico() : "")
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
        if (contexto.getEstadoActual() instanceof ec.edu.uce.certificadorforense.core.state.stateimplement.DatosObraState) {
            contexto.getEstadoActual().avanzar(contexto); // Avanza a FirmaAutorState
        }

        String ipRegistro = (String) body.get("ip_registro");

        // PERSISTENCIA EN DB (Soporte para rectificación)
        Optional<ExpedienteResumen> expDbOpt = expedienteRepository.buscarResumenPorId(idExpediente);

        if (expDbOpt.isEmpty()) {
            // Verificar si el archivo ya fue subido en otro intento (hash duplicado)
            Optional<ExpedienteResumen> expAnteriorOpt = expedienteRepository.buscarPorHashPsd(contexto.getSha512PSD());

            if (expAnteriorOpt.isPresent()) {
                ExpedienteResumen expAnterior = expAnteriorOpt.get();
                if (esDuplicadoBloqueante(expAnterior)) {
                    String mensajeError;
                    if (expAnterior.getUsuarioId().equals(usuarioDb.getId())) {
                        mensajeError = "El archivo PSD original ya se encuentra certificado en el sistema. No puedes certificar la misma obra dos veces.";
                    } else {
                        mensajeError = "ALERTA DE SEGURIDAD: Esta obra ya se encuentra certificada y pertenece a otro autor.";
                        // Registrar en el historial en una transacción independiente para que no se
                        // deshaga al lanzar la excepción
                        expedienteRepository.registrarAlertaSeguridad(expAnterior.getObraId(), usuarioDb.getCedula(),
                                usuarioDb.getNombres(), usuarioDb.getApellidos());
                    }
                    throw new RuntimeException(mensajeError);
                } else {
                    // Es un borrador huérfano (el usuario regresó al Paso 1 y subió el mismo
                    // archivo). Eliminamos el borrador anterior para permitir este nuevo intento.
                    expedienteRepository.eliminarBorrador(expAnterior.getObraId());
                }
            }

            com.google.gson.JsonObject jsonEv = new com.google.gson.JsonObject();
            if (contexto.getArchivoImagen() != null && contexto.getArchivoImagen().getMetadatos() != null) {
                jsonEv.addProperty("ancho", contexto.getArchivoImagen().getMetadatos().getAncho());
                jsonEv.addProperty("alto", contexto.getArchivoImagen().getMetadatos().getAlto());
                jsonEv.addProperty("extensionReal", contexto.getArchivoImagen().getMetadatos().getExtensionReal());
            }

            expedienteRepository.registrarNuevaObraYExpediente(idExpediente, usuarioDb.getId(), obra, cat, decl,
                    ipRegistro, contexto.getSha512PSD(), contexto.getSha512Imagen(), contexto.getPHash(),
                    jsonEv.toString());
        } else {
            // Rectificación: Actualizar obra existente
            expedienteRepository.actualizarObraYDeclaraciones(idExpediente, usuarioDb.getId(), obra, cat, decl,
                    ipRegistro);
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

        ExpedienteResumen expDb = expedienteRepository.buscarResumenPorId(idExpediente)
                .orElseThrow(() -> new RuntimeException("Expediente no encontrado en BD."));

        String p12Base64 = expDb.getUsuarioFirmaP12();
        if (p12Base64 == null || p12Base64.isEmpty()) {
            throw new RuntimeException("El usuario no tiene una firma digital configurada en el sistema.");
        }

        // Desciframos el sobre criptográfico de la credencial .p12 si estaba enmascarada
        if (vaultEncryptionService != null) {
            p12Base64 = vaultEncryptionService.decrypt(p12Base64);
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
            firmaBase64 = firmadorNube.firmar(p12Base64, password, hashObra);
        } catch (FirmadorNubePort.FirmaNubeException fne) {
            throw new RuntimeException("Error de firma digital en la nube: " + fne.getMessage(), fne);
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

        if (contexto.getEstadoActual() instanceof ec.edu.uce.certificadorforense.core.state.stateimplement.FirmaAutorState) {
            contexto.getEstadoActual().avanzar(contexto); // Avanza a CertificadoState
        }

        expedienteRepository.guardarFirma(idExpediente, firma);

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

        CertificadoService certServ = new CertificadoService(qrPort, hashPort);

        String expedienteFirmadoJson = contexto.getExpedienteJson() + "\n---FIRMA---\n"
                + contexto.getFirmaAutor().getFirmaBase64();
        Certificado certificado = certServ.generar(contexto.getExpediente(), expedienteFirmadoJson);
        contexto.setCertificado(certificado);

        String imagenBase64 = Base64.getEncoder().encodeToString(contexto.getImagenRaw());

        byte[] pdfSinFirmar = generadorPDF.generar(certificado, contexto.getExpediente(), expedienteFirmadoJson,
                imagenBase64);

        byte[] pdfFirmado = pdfSinFirmar;
        try {
            pdfFirmado = selladorInstitucional.sellar(pdfSinFirmar, certPassword);
        } catch (Exception ignored) {
        }
        contexto.setPdfCertificado(pdfFirmado);

        // Inyección de Datos de Certificación en la Imagen
        String ext = contexto.getArchivoImagen().getMetadatos() != null && contexto.getArchivoImagen().getMetadatos().getExtensionReal() != null
                ? contexto.getArchivoImagen().getMetadatos().getExtensionReal().toLowerCase()
                : contexto.getArchivoImagen().getNombreArchivo().toLowerCase();
        InyeccionDatosPort inyector = (ext.contains("jpg") || ext.contains("jpeg")) ? inyeccionJpeg : inyeccionPng;

        String jsonInyeccion = "{\"id\":\"" + certificado.getIdCertificado() + "\",\"hash\":\""
                + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = inyector.inyectar(contexto.getImagenRaw(), jsonInyeccion);
        contexto.setImagenCertificada(imagenCert);

        expedienteRepository.guardarCertificado(idExpediente, certificado, expedienteFirmadoJson);

        // Generar Zip
        java.io.ByteArrayOutputStream baosZip = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baosZip);

        java.util.zip.ZipEntry pdfEntry = new java.util.zip.ZipEntry(certificado.getIdCertificado() + ".pdf");
        zos.putNextEntry(pdfEntry);
        zos.write(pdfFirmado);
        zos.closeEntry();

        String imgExt = ext.contains("jpg") || ext.contains("jpeg") ? ".jpg" : ".png";
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
        contextoCreadoEn.remove(idExpediente);
        return zip;
    }
}
