package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Procesador específico para archivos de Photoshop (PSD).
 * Extrae metadatos y capas estructurales utilizando servicios delegados.
 */
public class ArchivoPSDProcessor implements ArchivoProcessorPort<ArchivoPSD> {

    private final MetadatosPSDService metadatosService;

    public ArchivoPSDProcessor() {
        this.metadatosService = new MetadatosPSDService();
    }

    @Override
    public ArchivoPSD procesar(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo es inválido o no existe.");
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            bytes = new byte[0];
        }

        // Extraer metadatos y capas estructurales en paralelo
        java.util.concurrent.CompletableFuture<MetadatosPSD> futureMetadatos = java.util.concurrent.CompletableFuture.supplyAsync(() -> metadatosService.procesarArchivo(file));
        java.util.concurrent.CompletableFuture<List<EstructuraCapaPSD>> futureCapas = java.util.concurrent.CompletableFuture.supplyAsync(() -> ExtractorCapasPSD.extraer(file.getAbsolutePath()));

        java.util.concurrent.CompletableFuture.allOf(futureMetadatos, futureCapas).join();

        MetadatosPSD metadatos = futureMetadatos.join();
        List<EstructuraCapaPSD> capas = futureCapas.join();

        // Fusionar en el objeto del dominio
        return ArchivoPSD.builder()
                .nombreArchivo(file.getName())
                .contenidoBytes(bytes)
                .tamanoBytes(file.length())
                .metadatos(metadatos)
                .capas(capas)
                .build();
    }

    @Override
    public boolean soporta(File file) {
        String formato = NumerosMagicos.detectarFormatoReal(file);
        return "PSD".equals(formato);
    }
}

