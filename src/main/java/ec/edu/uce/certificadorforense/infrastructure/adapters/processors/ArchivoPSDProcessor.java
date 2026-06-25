package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.model.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.psd.MetadatosPSD;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;

import java.io.File;
import java.util.List;

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

        // 1. Extraer metadatos
        MetadatosPSD metadatos = metadatosService.procesarArchivo(file);

        // 2. Extraer capas estructurales reales por stream parcial
        List<EstructuraCapaPSD> capas = ExtractorCapasPSD.extraer(file.getAbsolutePath());

        // 3. Fusionar en el objeto del dominio
        return ArchivoPSD.builder()
                .nombreArchivo(file.getName())
                .rutaAbsoluta(file.getAbsolutePath())
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
