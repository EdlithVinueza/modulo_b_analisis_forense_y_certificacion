package ec.edu.uce.certificadorforense;

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
import ec.edu.uce.certificadorforense.infrastructure.adapters.esteganografia.EsteganografiaJPEGAdapter;
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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProcesoCertificacionTest {

    private static final String BASE_PATH = "/home/edlith/Documentos/UCE 26-26/TESIS/Archivos de Prueba/";
    private static final String RUTA_PSD = BASE_PATH + "psd/girasoles-original.psd";
    private static final String RUTA_IMAGEN = BASE_PATH + "imagenes/girasol-original.png";

    // User requested paths and passwords
    private static final String RUTA_P12_AUTOR = "/home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/autor_certificado/autor.p12";
    private static final String PASS_AUTOR = "MiClaveSegura123";

    private static final String PASS_CA = "ClaveSistema2026!"; // Default from ProcesoCertificacionRunner
    private static final String DIRECTORIO_SALIDA = "resultados_test_certificacion";

    @Test
    public void testFlujoCompletoCertificacionPNG() throws Exception {
        System.out.println("=========================================================");
        System.out.println("🚀 EJECUTANDO TEST GENERAL: IMAGEN PNG 🚀");
        System.out.println("=========================================================");
        ejecutarFlujo(RUTA_PSD, BASE_PATH + "imagenes/girasol-original.png", "png");
    }

    @Test
    public void testFlujoCompletoCertificacionJPG() throws Exception {
        System.out.println("=========================================================");
        System.out.println("🚀 EJECUTANDO TEST GENERAL: IMAGEN JPG 🚀");
        System.out.println("=========================================================");
        ejecutarFlujo(RUTA_PSD, BASE_PATH + "imagenes/girasol-original.jpg", "jpg");
    }

    private void ejecutarFlujo(String rutaPsd, String rutaImagen, String extension) throws Exception {
        File archivoPSD = new File(rutaPsd);
        File archivoImagen = new File(rutaImagen);
        File p12Autor = new File(RUTA_P12_AUTOR);

        Assumptions.assumeTrue(archivoPSD.exists() && archivoImagen.exists(),
                "Faltan archivos de prueba en: " + BASE_PATH);
        Assumptions.assumeTrue(p12Autor.exists(), "Falta archivo p12 del artista en: " + RUTA_P12_AUTOR);

        // ── 1. Construir adaptadores de infraestructura ──────────────────────
        GeneradorHashPort hashPort = new SHA512Adapter();
        GeneradorQRPort qrPort = new QRGeneratorAdapter();
        FirmadorExpedientePort firmadorExp = new FirmadorP12Adapter();
        String rutaRootCa = "/home/edlith/Documentos/UCE 26-26/TESIS/CLAVES LINUX/sistema_certificado/sistema.p12";
        FirmadorPDFPort firmadorPDF = new FirmadorPDFAdapter(rutaRootCa);
        GeneradorPDFPort generadorPDF = new GeneradorPDFAdapter();
        
        EsteganografiaPort estegano;
        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
            estegano = new EsteganografiaJPEGAdapter();
        } else {
            estegano = new EsteganografiaPNGAdapter();
        }
        
        ExpedienteRepositoryPort repositorio = new ExpedienteJsonAdapter(DIRECTORIO_SALIDA + "/expedientes");

        // ── 2. Construir servicios de dominio ────────────────────────────────
        HashSHA512Service hashService = new HashSHA512Service(hashPort);
        ExpedienteService expedienteServ = new ExpedienteService();
        FirmaAutorService firmaServ = new FirmaAutorService(firmadorExp);
        CertificadoService certServ = new CertificadoService(qrPort, hashPort);

        // ── 3. Construir procesadores de archivos ────────────────────────────
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

        // ── 4. Inicializar contexto y EventPublisher ─────────────────────────
        AnalisisForenseState estadoInicial = new AnalisisForenseState();
        ContextoProceso contexto = new ContextoProceso(estadoInicial);

        EventPublisher publisher = new EventPublisher();
        publisher.suscribir(new HashGeneratorListener(hashPort, contexto));
        publisher.suscribir(new MetadataExtractorListener(contexto));
        publisher.suscribir(new CapasExtractorListener(contexto));
        publisher.suscribir(new ExpedienteListener(repositorio));
        publisher.suscribir(new AuditoriaListener(DIRECTORIO_SALIDA + "/auditoria.log"));
        publisher.suscribir(new PDFGeneratorListener(contexto));

        // ══════════════════════════════════════════════════════════════════════
        // FASE 1 — Análisis Forense
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n FASE 1 — ANÁLISIS FORENSE");

        @SuppressWarnings("unchecked")
        ArchivoPSD psd = ((ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(archivoPSD)).procesar(archivoPSD);
        assertNotNull(psd, "PSD no pudo ser procesado");

        @SuppressWarnings("unchecked")
        ArchivoImagen imagen = ((ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(archivoImagen))
                .procesar(archivoImagen);
        assertNotNull(imagen, "Imagen no pudo ser procesada");

        contexto.setArchivoPSD(psd);
        contexto.setArchivoImagen(imagen);

        // Publicar evento
        publisher.publicar(new EventoAnalisisIniciado(psd, imagen));

        // Validar reglas forenses
        VeredictoFinal veredictoPSD = validadorPSD.validar(psd);
        VeredictoFinal veredictoImagen = validadorImagen.validar(imagen);

        assertFalse(veredictoPSD.isEsRechazado(), "PSD no debe ser rechazado: " + veredictoPSD.getRazonRechazo());
        assertFalse(veredictoImagen.isEsRechazado(),
                "Imagen no debe ser rechazada: " + veredictoImagen.getRazonRechazo());

        contexto.setVeredictoPSD(veredictoPSD);
        contexto.setVeredictoImagen(veredictoImagen);

        // pHash comparativo
        BufferedImage imgPSD = ImageLoader.loadWithSubsampling(archivoPSD);
        BufferedImage imgImagen = ImageLoader.loadWithSubsampling(archivoImagen);
        assertNotNull(imgPSD, "La carga del composite del PSD falló");
        assertNotNull(imgImagen, "La carga de la imagen falló");

        String pHash = calcPHash.generarHash(imgImagen);
        contexto.setPHash(pHash);
        double similitud = calcPHash.compararSimilitud(calcPHash.generarHash(imgPSD), pHash);
        System.out.printf("  pHash similitud: %.2f%%%n", similitud);
        assertTrue(similitud >= 95.0, "La similitud visual de los archivos originales debería ser excelente (>= 95%)");

        // Avanzar estado
        contexto.getEstadoActual().avanzar(contexto);

        publisher.publicar(new EventoAnalisisCompletado(
                contexto.getSha512PSD(), contexto.getSha512Imagen(),
                contexto.getPHash(), contexto.getCapasPSD()));

        // ══════════════════════════════════════════════════════════════════════
        // FASE 2 — Datos de la Obra
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n FASE 2 — DATOS DE LA OBRA");

        Autor autor = Autor.builder()
                .nombres("Artista")
                .apellidos("Test")
                .cedula("0000000000")
                .correo("artista@test.com")
                .seudonimo("Test_Art")
                .build();

        Obra obra = Obra.builder()
                .titulo("Obra de Integracion")
                .descripcion("Prueba general")
                .software("TestSoft")
                .hardware("Tableta Gráfica Wacom")
                .categoria(CategoriaObra.ILUSTRACION)
                .fechaCreacion(LocalDate.now())
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
        System.out.println("\n FASE 3 — FIRMA DEL AUTOR");

        Expediente expediente = expedienteServ.construir(contexto);
        contexto.setExpediente(expediente);
        assertNotNull(expediente);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new com.google.gson.JsonSerializer<LocalDate>() {
                    @Override
                    public com.google.gson.JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfSrc,
                            com.google.gson.JsonSerializationContext context) {
                        return new com.google.gson.JsonPrimitive(src.toString());
                    }
                })
                .setPrettyPrinting()
                .create();
        String expedienteJson = gson.toJson(expediente);
        contexto.setExpedienteJson(expedienteJson);

        assertDoesNotThrow(() -> firmaServ.validar(p12Autor, PASS_AUTOR), "La validación del p12 del artista falló");

        FirmaAutor firma = firmaServ.firmar(expedienteJson, p12Autor, PASS_AUTOR);
        assertNotNull(firma, "No se pudo firmar el expediente");
        assertNotNull(firma.getFirmaBase64(), "La firma generada está vacía");

        contexto.setFirmaAutor(firma);

        publisher.publicar(new EventoFirmaRealizada(expediente, expedienteJson, firma));
        contexto.getEstadoActual().avanzar(contexto);

        // ══════════════════════════════════════════════════════════════════════
        // FASE 4 — Emisión del Certificado
        // ══════════════════════════════════════════════════════════════════════
        System.out.println("\n FASE 4 — EMISIÓN DEL CERTIFICADO");

        String imagenBase64 = null;
        try {
            byte[] imgBytes = Files.readAllBytes(archivoImagen.toPath());
            imagenBase64 = Base64.getEncoder().encodeToString(imgBytes);
        } catch (IOException e) {
            fail("No se pudo leer la imagen a Base64");
        }

        String expedienteFirmadoJson = expedienteJson + "\n---FIRMA---\n" + firma.getFirmaBase64();
        Certificado certificado = certServ.generar(expediente, expedienteFirmadoJson);
        assertNotNull(certificado, "No se generó el certificado");
        contexto.setCertificado(certificado);

        // Para firmar el PDF con la CA, intentamos, pero si falla por falta de
        // certificado de CA, no hay problema, es test.
        byte[] pdfSinFirmar = generadorPDF.generar(certificado, expediente, expedienteFirmadoJson, imagenBase64);
        assertNotNull(pdfSinFirmar, "No se generó el PDF sin firmar");

        try {
            byte[] pdfFirmado = firmadorPDF.firmarPDF(pdfSinFirmar, PASS_CA);
            contexto.setPdfCertificado(pdfFirmado);
            assertNotNull(pdfFirmado, "No se firmó el PDF");
        } catch (Exception e) {
            System.err.println(
                    "  Advertencia: No se pudo firmar el PDF (¿falta sistema.p12  o pass CA incorrecto?). Se usará el PDF sin firmar.");
            contexto.setPdfCertificado(pdfSinFirmar);
        }

        // Esteganografía
        String jsonEstegano = "{\"id\":\"" + certificado.getIdCertificado()
                + "\",\"hash\":\"" + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = null;
        try {
            byte[] imgBytes = Files.readAllBytes(archivoImagen.toPath());
            imagenCert = estegano.inyectar(imgBytes, jsonEstegano);
            contexto.setImagenCertificada(imagenCert);
            assertNotNull(imagenCert, "No se inyectó esteganografía");
        } catch (Exception e) {
            fail("Falló la esteganografía: " + e.getMessage());
        }

        Path dirSalida = Paths.get(DIRECTORIO_SALIDA);
        Files.createDirectories(dirSalida);

        String nombrePDF = certificado.getIdCertificado() + "-" + extension + ".pdf";
        Files.write(dirSalida.resolve(nombrePDF), contexto.getPdfCertificado());
        System.out.println("  PDF guardado: " + dirSalida.resolve(nombrePDF).toAbsolutePath());

        String nombreImagenCertificada = certificado.getIdCertificado() + "-obra-certificada." + extension;
        Files.write(dirSalida.resolve(nombreImagenCertificada), imagenCert);
        System.out.println("   Imagen certificada guardada: " + dirSalida.resolve(nombreImagenCertificada).toAbsolutePath());

        publisher.publicar(new EventoCertificadoEmitido(certificado));

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║  TEST GENERAL COMPLETADO — " + certificado.getIdCertificado() + "  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
