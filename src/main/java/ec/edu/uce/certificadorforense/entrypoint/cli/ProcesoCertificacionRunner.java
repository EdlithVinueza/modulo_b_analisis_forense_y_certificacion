package ec.edu.uce.certificadorforense.entrypoint.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import ec.edu.uce.certificadorforense.core.observer.listeners.*;
import ec.edu.uce.certificadorforense.core.ports.out.*;
import ec.edu.uce.certificadorforense.core.service.*;
import ec.edu.uce.certificadorforense.core.state.*;
import ec.edu.uce.certificadorforense.core.rules.imagen.*;
import ec.edu.uce.certificadorforense.core.rules.psd.*;
import ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia.EsteganografiaPNGAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorP12Adapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.firma.FirmadorPDFAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.hash.SHA512Adapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.json.ExpedienteJsonAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.pdf.GeneradorPDFAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.*;
import ec.edu.uce.certificadorforense.infrastructure.adapters.qr.QRGeneratorAdapter;
import ec.edu.uce.certificadorforense.core.service.ArchivoProcessorFactory;
import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import ec.edu.uce.certificadorforense.core.service.ValidadorGenericoService;
import ec.edu.uce.certificadorforense.core.service.CalculadorPHash;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Entrypoint CLI temporal del sistema Verisart.
 * <p>
 * Actúa como <strong>adaptador de entrada CLI</strong> — ensambla todas las dependencias
 * (adaptadores de infraestructura + servicios de dominio + estados) e inicia el flujo.
 * </p>
 *
 * <p><strong>Cuando se migre a REST (Spring Boot/Quarkus):</strong>
 * este runner se reemplaza por un {@code @RestController} o {@code @ApplicationScoped}
 * sin modificar ninguna clase del {@code core}.</p>
 *
 * <p><strong>Uso temporal para pruebas:</strong> configure las rutas y datos en las
 * constantes de la sección "CONFIGURACIÓN DE PRUEBA".</p>
 */
public class ProcesoCertificacionRunner {

    // ── CONFIGURACIÓN DE PRUEBA ──────────────────────────────────────────────
    private static final String RUTA_PSD     = "src/main/resources/archivos_psd_prueba/Chica de cabello y girasoles - 05-02-2026.psd";
    private static final String RUTA_IMAGEN  = "src/main/resources/imagenes_prueba/original.png";
    private static final String RUTA_P12_AUTOR = "/home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/autor_certificado/autor.p12";
    private static final String PASS_AUTOR   = "cambiarme";
    private static final String PASS_CA      = "cambiarme";
    private static final String DIRECTORIO_SALIDA = "resultados certificacion";

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║     SISTEMA VERISART — Certificación Forense         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // ── 1. Construir adaptadores de infraestructura ──────────────────────
        GeneradorHashPort        hashPort       = new SHA512Adapter();
        GeneradorQRPort          qrPort         = new QRGeneratorAdapter();
        FirmadorExpedientePort   firmadorExp    = new FirmadorP12Adapter();
        String rutaRootCa = "/home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/sistema_certificado/sistema.p12";
        FirmadorPDFPort          firmadorPDF    = new FirmadorPDFAdapter(rutaRootCa);
        GeneradorPDFPort         generadorPDF   = new GeneradorPDFAdapter();
        EsteganografiaPort       estegano       = new EsteganografiaPNGAdapter();
        ExpedienteRepositoryPort repositorio    = new ExpedienteJsonAdapter();

        // ── 2. Construir servicios de dominio ────────────────────────────────
        HashSHA512Service     hashService    = new HashSHA512Service(hashPort);
        ExpedienteService     expedienteServ = new ExpedienteService();
        FirmaAutorService     firmaServ      = new FirmaAutorService(firmadorExp);
        CertificadoService    certServ       = new CertificadoService(qrPort, hashPort);

        // ── 3. Construir procesadores de archivos (ya existentes) ────────────
        List<ArchivoProcessorPort<? extends ArchivoBase>> procesadores = Arrays.asList(
                new ArchivoImagenProcessor(),
                new ArchivoPSDProcessor()
        );
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

        // ── 4. Inicializar contexto y EventPublisher ─────────────────────────
        AnalisisForenseState estadoInicial = new AnalisisForenseState();
        ContextoProceso contexto = new ContextoProceso(estadoInicial);

        EventPublisher publisher = new EventPublisher();
        publisher.suscribir(new HashGeneratorListener(hashPort, contexto));
        publisher.suscribir(new MetadataExtractorListener(contexto));
        publisher.suscribir(new CapasExtractorListener(contexto));
        publisher.suscribir(new ExpedienteListener(repositorio));
        publisher.suscribir(new AuditoriaListener());
        publisher.suscribir(new PDFGeneratorListener(contexto));

        // ══════════════════════════════════════════════════════════════════════
        // FASE 1 — Análisis Forense
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n FASE 1 — ANÁLISIS FORENSE");
        File archivoPSD   = new File(RUTA_PSD);
        File archivoImagen = new File(RUTA_IMAGEN);

        @SuppressWarnings("unchecked")
        ArchivoPSD psd = ((ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(archivoPSD))
                .procesar(archivoPSD);
        @SuppressWarnings("unchecked")
        ArchivoImagen imagen = ((ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(archivoImagen))
                .procesar(archivoImagen);

        contexto.setArchivoPSD(psd);
        contexto.setArchivoImagen(imagen);

        // Publicar evento → dispara HashGenerator, MetadataExtractor, CapasExtractor
        publisher.publicar(new EventoAnalisisIniciado(psd, imagen));

        // Validar reglas forenses
        VeredictoFinal veredictoPSD    = validadorPSD.validar(psd);
        VeredictoFinal veredictoImagen = validadorImagen.validar(imagen);
        contexto.setVeredictoPSD(veredictoPSD);
        contexto.setVeredictoImagen(veredictoImagen);

        // pHash comparativo
        BufferedImage imgPSD   = ImageLoader.loadWithSubsampling(archivoPSD);
        BufferedImage imgImagen = ImageLoader.loadWithSubsampling(archivoImagen);
        if (imgPSD != null && imgImagen != null) {
            String pHash = calcPHash.generarHash(imgImagen);
            contexto.setPHash(pHash);
            double similitud = calcPHash.compararSimilitud(
                    calcPHash.generarHash(imgPSD), pHash);
            System.out.printf("  pHash similitud: %.2f%%%n", similitud);
        }

        // Avanzar estado
        contexto.getEstadoActual().avanzar(contexto);

        publisher.publicar(new EventoAnalisisCompletado(
                contexto.getSha512PSD(), contexto.getSha512Imagen(),
                contexto.getPHash(), contexto.getCapasPSD()));

        // ══════════════════════════════════════════════════════════════════════
        // FASE 2 — Datos de la Obra
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n▶ FASE 2 — DATOS DE LA OBRA");

        Autor autor = Autor.builder()
                .nombres("Edlith")
                .apellidos("Vinueza")
                .cedula("1234567890")
                .correo("edlith@uce.edu.ec")
                .seudonimo("EV_Art")
                .build();

        Obra obra = Obra.builder()
                .titulo("Chica de cabello y girasoles")
                .descripcion("Ilustración digital de personaje femenino con elementos florales")
                .software("Adobe Photoshop")
                .categoria(CategoriaObra.ILUSTRACION)
                .fechaCreacion(LocalDate.of(2026, 2, 5))
                .build();

        Declaraciones declaraciones = Declaraciones.builder()
                .titularDerechos(true)

                .aceptaTerminos(true)
                .build();

        contexto.setAutor(autor);
        contexto.setObra(obra);
        contexto.setDeclaraciones(declaraciones);
        contexto.getEstadoActual().avanzar(contexto);

        // ══════════════════════════════════════════════════════════════════════
        // FASE 3 — Firma del Autor
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n▶ FASE 3 — FIRMA DEL AUTOR");

        Expediente expediente = expedienteServ.construir(contexto);
        contexto.setExpediente(expediente);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String expedienteJson = gson.toJson(expediente);
        contexto.setExpedienteJson(expedienteJson);
        System.out.println("  Expediente JSON:\n" + expedienteJson);

        File p12Autor = new File(RUTA_P12_AUTOR);
        firmaServ.validar(p12Autor, PASS_AUTOR);
        FirmaAutor firma = firmaServ.firmar(expedienteJson, p12Autor, PASS_AUTOR);
        contexto.setFirmaAutor(firma);

        publisher.publicar(new EventoFirmaRealizada(expediente, expedienteJson, firma));
        contexto.getEstadoActual().avanzar(contexto);

        // ══════════════════════════════════════════════════════════════════════
        // FASE 4 — Emisión del Certificado
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n▶ FASE 4 — EMISIÓN DEL CERTIFICADO");

        // Leer imagen para base64
        String imagenBase64 = null;
        try {
            byte[] imgBytes = Files.readAllBytes(archivoImagen.toPath());
            imagenBase64 = Base64.getEncoder().encodeToString(imgBytes);
        } catch (IOException e) {
            System.err.println("  Advertencia: no se pudo leer la imagen: " + e.getMessage());
        }

        // Generar certificado
        String expedienteFirmadoJson = expedienteJson + "\n---FIRMA---\n" + firma.getFirmaBase64();
        Certificado certificado = certServ.generar(expediente, expedienteFirmadoJson);
        contexto.setCertificado(certificado);

        // Generar PDF
        byte[] pdfSinFirmar = generadorPDF.generar(certificado, expediente, expedienteFirmadoJson, imagenBase64);
        byte[] pdfFirmado   = firmadorPDF.firmarPDF(pdfSinFirmar, PASS_CA);
        contexto.setPdfCertificado(pdfFirmado);

        // Inyectar JSON en imagen PNG
        String jsonEstegano = "{\"id\":\"" + certificado.getIdCertificado()
                + "\",\"hash\":\"" + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = null;
        try {
            byte[] imgBytes = Files.readAllBytes(archivoImagen.toPath());
            imagenCert = estegano.inyectar(imgBytes, jsonEstegano);
            contexto.setImagenCertificada(imagenCert);
        } catch (Exception e) {
            System.err.println("  Advertencia esteganografía: " + e.getMessage());
        }

        // Guardar archivos de salida
        Path dirSalida = Paths.get(DIRECTORIO_SALIDA);
        Files.createDirectories(dirSalida);

        String nombrePDF = certificado.getIdCertificado() + ".pdf";
        Files.write(dirSalida.resolve(nombrePDF), pdfFirmado);
        System.out.println("  ✅ PDF generado: " + dirSalida.resolve(nombrePDF).toAbsolutePath());

        if (imagenCert != null) {
            String nombrePNG = "obra-certificada.png";
            Files.write(dirSalida.resolve(nombrePNG), imagenCert);
            System.out.println("  ✅ PNG certificado: " + dirSalida.resolve(nombrePNG).toAbsolutePath());
        }

        publisher.publicar(new EventoCertificadoEmitido(certificado));

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║  PROCESO COMPLETADO — " + certificado.getIdCertificado() + "  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
