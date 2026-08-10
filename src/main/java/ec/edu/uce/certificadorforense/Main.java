package ec.edu.uce.certificadorforense;

import ec.edu.uce.certificadorforense.core.model.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaAnalisisOrigen;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaCoherenciaDpi;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaFirmaEstructural;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaComplejidadDiseno;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaFormatoPsd;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaImagenPegada;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaResolucionProfesional;
import ec.edu.uce.certificadorforense.core.service.ArchivoProcessorFactory;
import ec.edu.uce.certificadorforense.core.service.ValidadorGenericoService;
import ec.edu.uce.certificadorforense.core.service.CalculadorPHash;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ArchivoImagenProcessor;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ArchivoPSDProcessor;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ImageLoader;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.*;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("🚀 INICIANDO PRUEBA DE RENDIMIENTO Y COMPARACIÓN FORENSE 🚀");
        System.out.println("=========================================================");

        // 1. Inicializar la fábrica de procesadores
        List<ArchivoProcessorPort<? extends ArchivoBase>> procesadores = Arrays.asList(
                new ArchivoImagenProcessor(),
                new ArchivoPSDProcessor()
        );
        ArchivoProcessorFactory factory = new ArchivoProcessorFactory(procesadores);

        // 2. Inicializar los validadores forenses
        ValidadorGenericoService<ArchivoImagen> validadorImagen = new ValidadorGenericoService<>();
        validadorImagen.registrarRegla(new ReglaFirmaEstructural());
        validadorImagen.registrarRegla(new ReglaCoherenciaDpi());
        validadorImagen.registrarRegla(new ReglaAnalisisOrigen());

        ValidadorGenericoService<ArchivoPSD> validadorPSD = new ValidadorGenericoService<>();
        validadorPSD.registrarRegla(new ReglaFormatoPsd());
        validadorPSD.registrarRegla(new ReglaResolucionProfesional());
        validadorPSD.registrarRegla(new ReglaImagenPegada());
        validadorPSD.registrarRegla(new ReglaComplejidadDiseno());

        // 3. Instanciar el comparador de similitud perceptual por pHash
        CalculadorPHash calculadorPHash = new CalculadorPHash();

        // 4. Obtener archivos a procesar desde argumentos CLI si se proveen
        if (args.length < 2) {
            System.out.println("\nUso del Benchmark Forense: java -jar app.jar <ruta-archivo.psd> <ruta-imagen.png|jpg>");
            System.out.println("No se especificaron archivos de entrada por consola.");
            return;
        }

        File archivoPSD = new File(args[0]);
        File archivoPNG = new File(args[1]);

        if (!archivoPSD.exists() || !archivoPNG.exists()) {
            System.err.println("Error: Uno o ambos archivos especificados no existen:");
            System.err.println("  • PSD: " + archivoPSD.getAbsolutePath());
            System.err.println("  • Imagen: " + archivoPNG.getAbsolutePath());
            return;
        }

        System.out.println("\nANALIZANDO ARCHIVOS EXTREMADAMENTE PESADOS...");
        System.out.println("• Archivo PSD: " + archivoPSD.getName() + " (" + (archivoPSD.length() / (1024 * 1024)) + " MB)");
        System.out.println("• Archivo PNG: " + archivoPNG.getName() + " (" + (archivoPNG.length() / (1024 * 1024)) + " MB)");

        long globalStart = System.currentTimeMillis();

        // A. Procesar y validar PSD
        System.out.println("\n--- [A. PROCESANDO PSD] ---");
        long startPSD = System.currentTimeMillis();
        ArchivoPSD psd = null;
        VeredictoFinal veredictoPSD = null;
        try {
            ArchivoProcessorPort<ArchivoPSD> processorPSD = (ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(archivoPSD);
            psd = processorPSD.procesar(archivoPSD);
            veredictoPSD = validadorPSD.validar(psd);

            System.out.println(psd.getMetadatos());
            System.out.println("• Capas binarias extraídas: " + psd.getCapas().size());
            psd.getCapas().stream().limit(5).forEach(c ->
                    System.out.println("  └─ Capa: [" + c.getNombre() + "] Tipo: " + c.getTipo() + " Blend: " + c.getBlendModeNombre())
            );
        } catch (Exception e) {
            System.err.println("Error procesando PSD: " + e.getMessage());
            e.printStackTrace();
        }
        long endPSD = System.currentTimeMillis();
        System.out.println("Tiempo PSD: " + (endPSD - startPSD) + " ms");

        // B. Procesar y validar PNG
        System.out.println("\n--- [B. PROCESANDO PNG] ---");
        long startPNG = System.currentTimeMillis();
        ArchivoImagen png = null;
        VeredictoFinal veredictoPNG = null;
        try {
            ArchivoProcessorPort<ArchivoImagen> processorPNG = (ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(archivoPNG);
            png = processorPNG.procesar(archivoPNG);
            veredictoPNG = validadorImagen.validar(png);

            System.out.println(png.getMetadatos());
            System.out.println(png.getEstructura());
        } catch (Exception e) {
            System.err.println("Error procesando PNG: " + e.getMessage());
            e.printStackTrace();
        }
        long endPNG = System.currentTimeMillis();
        System.out.println("Tiempo PNG: " + (endPNG - startPNG) + " ms");

        // C. Similitud Perceptual Forense (si ambos son válidos)
        System.out.println("\n--- [C. COMPARACIÓN DE SIMILITUD PERCEPTUAL FORENSE (p-Hash)] ---");
        if (psd != null && png != null && !veredictoPSD.isEsRechazado() && !veredictoPNG.isEsRechazado()) {
            long startSim = System.currentTimeMillis();
            System.out.println("Extrayendo e indexando firmas visuales de manera optimizada en memoria...");

            // Carga submuestreada de alto rendimiento (consume casi 0 RAM al no decodificar el lienzo completo)
            BufferedImage imgPSD = ImageLoader.loadWithSubsampling(archivoPSD);
            BufferedImage imgPNG = ImageLoader.loadWithSubsampling(archivoPNG);

            if (imgPSD != null && imgPNG != null) {
                // Generar hash perceptual (pHash)
                String hashPSD = calculadorPHash.generarHash(imgPSD);
                String hashPNG = calculadorPHash.generarHash(imgPNG);

                // Comparar similitud de hashes por distancia Hamming
                double porcentajeSimilitud = calculadorPHash.compararSimilitud(hashPSD, hashPNG);
                long endSim = System.currentTimeMillis();

                System.out.println("\nREPORTE DE SIMILITUD FORENSE:");
                System.out.println("• Hash Perceptual PSD composite: " + hashPSD);
                System.out.println("• Hash Perceptual PNG subida   : " + hashPNG);
                System.out.printf("• Porcentaje de Coincidencia  : %.2f%%%n", porcentajeSimilitud);
                System.out.println("Tiempo Extracción/Comparación: " + (endSim - startSim) + " ms");

                if (porcentajeSimilitud >= 95.0) {
                    System.out.println("\nVEREDICTO FINAL DE AUTENTICIDAD: EXCELENTE");
                    System.out.println("  La imagen PNG coincide plenamente con la composición pre-renderizada del archivo PSD.");
                } else if (porcentajeSimilitud >= 80.0) {
                    System.out.println("\nVEREDICTO FINAL DE AUTENTICIDAD: SOSPECHOSO / MODIFICADO");
                    System.out.println("  Hay alta similitud, pero se detectan variaciones de encuadre, color o edición.");
                } else {
                    System.err.println("\nVEREDICTO FINAL DE AUTENTICIDAD: FRAUDE DETECTADO");
                    System.err.println("  Las firmas perceptuales no coinciden en absoluto. La imagen subida no procede de este PSD.");
                }
            } else {
                System.err.println("Fallo crítico al cargar composiciones visuales.");
            }
        } else {
            System.err.println("No se realiza comparación forense: Uno o ambos archivos fallaron las reglas de autenticidad base.");
            if (veredictoPSD != null && veredictoPSD.isEsRechazado()) {
                System.err.println("  └─ PSD Rechazado por: " + veredictoPSD.getRazonRechazo());
            }
            if (veredictoPNG != null && veredictoPNG.isEsRechazado()) {
                System.err.println("  └─ PNG Rechazada por: " + veredictoPNG.getRazonRechazo());
            }
        }

        long globalEnd = System.currentTimeMillis();
        System.out.println("\n=========================================================");
        System.out.printf("PROCESAMIENTO FORENSE INTEGRAL COMPLETADO EN: %d ms%n", (globalEnd - globalStart));
        System.out.println("=========================================================");
    }
}