package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.imagen.MetadatosImagen;
import ec.edu.uce.certificadorforense.core.model.imagen.EstructuraImagen;

import java.io.File;
import java.nio.file.Files;

/**
 * Procesador específico para archivos de imagen (PNG, JPEG).
 * Extrae metadatos y estructura física utilizando servicios delegados.
 */
public class ArchivoImagenProcessor implements ArchivoProcessorPort<ArchivoImagen> {

    private final MetadatosImagenService metadatosService;
    private final EstructuraImagenService estructuraService;

    public ArchivoImagenProcessor() {
        this.metadatosService = new MetadatosImagenService();
        this.estructuraService = new EstructuraImagenService();
    }

    @Override
    public ArchivoImagen procesar(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo es inválido o no existe.");
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            bytes = new byte[0];
        }

        // 1. Extraer metadatos
        MetadatosImagen metadatos = metadatosService.procesarArchivo(file);

        // 2. Extraer estructura binaria física
        EstructuraImagen estructura = estructuraService.construirAnalisis(file);

        // 3. Fusionar en el objeto del dominio
        return ArchivoImagen.builder()
                .archivo(file)
                .nombreArchivo(file.getName())
                .contenidoBytes(bytes)
                .tamanoBytes(file.length())
                .metadatos(metadatos)
                .estructura(estructura)
                .build();
    }

    @Override
    public boolean soporta(File file) {
        String formato = NumerosMagicos.detectarFormatoReal(file);
        return "PNG".equals(formato) || "JPEG".equals(formato);
    }
}

