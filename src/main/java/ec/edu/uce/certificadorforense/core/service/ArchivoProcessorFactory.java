package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.model.base.ArchivoBase;
import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;

import java.io.File;
import java.util.List;

public class ArchivoProcessorFactory {
    private final List<ArchivoProcessorPort<? extends ArchivoBase>> processors;

    public ArchivoProcessorFactory(List<ArchivoProcessorPort<? extends ArchivoBase>> processors) {
        this.processors = processors;
    }

    public ArchivoProcessorPort<? extends ArchivoBase> getProcessor(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo es nulo o no existe.");
        }
        return processors.stream()
                .filter(p -> p.soporta(file))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró un procesador para la firma binaria del archivo: " + file.getName()
                ));
    }
}
