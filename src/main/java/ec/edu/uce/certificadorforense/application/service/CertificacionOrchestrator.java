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


    // Caché en memoria para las peticiones stateless del Frontend
    private final Map<String, ContextoProceso> contextoCache = new ConcurrentHashMap<>();
    
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

    private final String RUTA_ROOT_CA = "/home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/sistema_certificado/sistema.p12";
    private final String PASS_CA = "ClaveSistema2026!";

    public String iniciarAnalisisFase1(File psdFile, File imgFile, String extension) throws Exception {
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
            ArchivoPSD psd = ((ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(psdFile)).procesar(psdFile);
            ArchivoImagen imagen = ((ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(imgFile)).procesar(imgFile);

            contexto.setArchivoPSD(psd);
            contexto.setArchivoImagen(imagen);
            contexto.setImagenRaw(Files.readAllBytes(imgFile.toPath())); // Guardar en RAM para evitar que /tmp lo borre

            VeredictoFinal veredictoPSD = validadorPSD.validar(psd);
            VeredictoFinal veredictoImagen = validadorImagen.validar(imagen);

            if (veredictoPSD.isEsRechazado()) throw new RuntimeException("Rechazo PSD: " + veredictoPSD.getRazonRechazo());
            if (veredictoImagen.isEsRechazado()) throw new RuntimeException("Rechazo Imagen: " + veredictoImagen.getRazonRechazo());

            contexto.setVeredictoPSD(veredictoPSD);
            contexto.setVeredictoImagen(veredictoImagen);

            // pHash
            BufferedImage imgPSD = ImageLoader.loadWithSubsampling(psdFile);
            BufferedImage imgImagen = ImageLoader.loadWithSubsampling(imgFile);
            String pHashStr = calcPHash.generarHash(imgImagen);
            contexto.setPHash(pHashStr);

            double similitud = calcPHash.compararSimilitud(calcPHash.generarHash(imgPSD), pHashStr);
            // Umbral a 90% para compensar las ligeras diferencias generadas por compresión
            if (similitud < 90.0) {
                throw new RuntimeException("Rechazo Imagen: La similitud visual pHash no es suficiente (" + String.format("%.2f", similitud) + "%). El PSD y la imagen no coinciden visualmente.");
            }

            contexto.setSha512PSD(hashPort.calcularSHA512(psdFile));
            contexto.setSha512Imagen(hashPort.calcularSHA512(imgFile));

            contexto.getEstadoActual().avanzar(contexto);

            contextoCache.put(expedienteId, contexto);

            return expedienteId;
        } finally {
            // Borrar archivos temporales de fase 1
            try { Files.deleteIfExists(psdFile.toPath()); } catch (Exception ignored) {}
            try { Files.deleteIfExists(imgFile.toPath()); } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void registrarDatosFase2(String idExpediente, UsuarioEntity usuarioDb, Map<String, Object> body) {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null) throw new RuntimeException("Expediente expirado o no existe.");

        Autor autor = Autor.builder()
                .nombres(usuarioDb.nombres)
                .apellidos(usuarioDb.apellidos)
                .cedula(usuarioDb.cedula)
                .correo(usuarioDb.correo)
                .seudonimo(usuarioDb.nombreArtistico != null ? usuarioDb.nombreArtistico : "")
                .build();

        String categoriaStr = (String) body.get("categoria");
        CategoriaObra cat = CategoriaObra.ILUSTRACION; // Default
        for(CategoriaObra c : CategoriaObra.values()) {
            if (c.name().equalsIgnoreCase(categoriaStr)) cat = c;
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
            ExpedienteForenseEntity expAnterior = ExpedienteForenseEntity.find("hashPsdOriginal", contexto.getSha512PSD()).firstResult();
            
            if (expAnterior != null) {
                if ("CERTIFICADO".equals(expAnterior.obra.estadoActual) || "FINALIZADO".equals(expAnterior.obra.estadoActual)) {
                    String mensajeError;
                    if (expAnterior.obra.usuario.id.equals(usuarioDb.id)) {
                        mensajeError = "El archivo PSD original ya se encuentra certificado en el sistema. No puedes certificar la misma obra dos veces.";
                    } else {
                        mensajeError = "ALERTA DE SEGURIDAD: Esta obra ya se encuentra certificada y pertenece a otro autor.";
                        // Registrar en el historial en una transacción independiente para que no se deshaga al lanzar la excepción
                        historialHelper.registrarAlerta(expAnterior.obra, usuarioDb.cedula, usuarioDb.nombres, usuarioDb.apellidos);
                    }
                    throw new RuntimeException(mensajeError);
                } else {
                    // Es un borrador huérfano (el usuario regresó al Paso 1 y subió el mismo archivo)
                    // Eliminamos el borrador anterior para permitir este nuevo intento.
                    HistorialEstadoEntity.delete("obra", expAnterior.obra);
                    FirmaAutorEntity.delete("expediente", expAnterior);
                    expAnterior.delete();
                    expAnterior.obra.delete();
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

            expDb = new ExpedienteForenseEntity();
            expDb.id = UUID.fromString(idExpediente);
            expDb.obra = obraEntity;
            expDb.hashPsdOriginal = contexto.getSha512PSD();
            expDb.hashImagenFinal = contexto.getSha512Imagen();
            expDb.similitudPhash = new java.math.BigDecimal("99.99");
            expDb.resultadoAnalisis = "APROBADO";
            expDb.evidenciaTecnicaJson = "{}";
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
    public String firmarFase3(String idExpediente, File p12File, String password) throws Exception {
        ContextoProceso contexto = contextoCache.get(idExpediente);
        if (contexto == null) throw new RuntimeException("Expediente expirado o no existe.");

        ExpedienteService expedienteServ = new ExpedienteService();
        Expediente expediente = expedienteServ.construir(contexto);
        contexto.setExpediente(expediente);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .setPrettyPrinting()
                .create();
        String expedienteJson = gson.toJson(expediente);
        contexto.setExpedienteJson(expedienteJson);

        FirmadorExpedientePort firmadorExp = new FirmadorP12Adapter();
        FirmaAutorService firmaServ = new FirmaAutorService(firmadorExp);
        
        firmaServ.validar(p12File, password);
        FirmaAutor firma = firmaServ.firmar(expedienteJson, p12File, password);
        contexto.setFirmaAutor(firma);

        if (contexto.getEstadoActual() instanceof ec.edu.uce.certificadorforense.core.state.FirmaAutorState) {
            contexto.getEstadoActual().avanzar(contexto); // Avanza a CertificadoState
        }

        // PERSISTENCIA DB
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        
        FirmaAutorEntity firmaDb = new FirmaAutorEntity();
        firmaDb.id = UUID.randomUUID();
        firmaDb.expediente = expDb;
        firmaDb.usuario = expDb.obra.usuario;
        firmaDb.hashFirmado = firma.getHashExpediente();
        firmaDb.firmaBase64 = firma.getFirmaBase64();
        firmaDb.algoritmo = firma.getAlgoritmo();
        firmaDb.fechaFirma = LocalDateTime.now();
        firmaDb.persist();

        HistorialEstadoEntity hist = new HistorialEstadoEntity();
        hist.id = UUID.randomUUID();
        hist.obra = expDb.obra;
        hist.estadoAnterior = "ESPERANDO_FIRMA";
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
        if (contexto == null) throw new RuntimeException("Expediente expirado o no existe.");

        GeneradorQRPort qrPort = new QRGeneratorAdapter();
        GeneradorHashPort hashPort = new SHA512Adapter();
        CertificadoService certServ = new CertificadoService(qrPort, hashPort);
        GeneradorPDFPort generadorPDF = new GeneradorPDFAdapter();

        String expedienteFirmadoJson = contexto.getExpedienteJson() + "\n---FIRMA---\n" + contexto.getFirmaAutor().getFirmaBase64();
        Certificado certificado = certServ.generar(contexto.getExpediente(), expedienteFirmadoJson);
        contexto.setCertificado(certificado);

        String imagenBase64 = Base64.getEncoder().encodeToString(contexto.getImagenRaw());

        byte[] pdfSinFirmar = generadorPDF.generar(certificado, contexto.getExpediente(), expedienteFirmadoJson, imagenBase64);
        
        FirmadorPDFPort firmadorPDF = new FirmadorPDFAdapter(RUTA_ROOT_CA);
        byte[] pdfFirmado = pdfSinFirmar;
        try {
            pdfFirmado = firmadorPDF.firmarPDF(pdfSinFirmar, PASS_CA);
        } catch(Exception e) {
            System.err.println("Fallback: No se pudo firmar el PDF con la CA del sistema. Enviando PDF sin firmar.");
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
        
        String jsonEstegano = "{\"id\":\"" + certificado.getIdCertificado() + "\",\"hash\":\"" + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = estegano.inyectar(contexto.getImagenRaw(), jsonEstegano);
        contexto.setImagenCertificada(imagenCert);

        // Persistencia Final DB
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.findById(UUID.fromString(idExpediente));
        CertificadoEntity certDb = new CertificadoEntity();
        certDb.id = UUID.randomUUID();
        certDb.obra = expDb.obra;
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
        hist.estadoAnterior = "CERTIFICADO";
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
        java.util.zip.ZipEntry imgEntry = new java.util.zip.ZipEntry(certificado.getIdCertificado() + "-obra-certificada" + imgExt);
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
