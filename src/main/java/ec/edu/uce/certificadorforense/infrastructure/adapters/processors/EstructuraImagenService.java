package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import ec.edu.uce.certificadorforense.core.model.imagen.EstructuraImagen;

import java.io.File;
import java.io.FileInputStream;

public class EstructuraImagenService {

    public EstructuraImagen construirAnalisis(File archivo) {
        // 1. Detectar formato real (PNG/JPEG/PSD/DESCONOCIDO)
        String formato = NumerosMagicos.detectarFormatoReal(archivo);

        // 2. Extraer DPIs desde los bytes (pHYs o JFIF)
        int[] dpis = ResolucionFisica.extraerDpi(archivo, formato);

        // 3. CONSTRUIR EL OBJETO USANDO EL BUILDER DE LOMBOK
        return EstructuraImagen.builder()
                .formatoReal(formato)
                .dpiX(dpis[0])
                .dpiY(dpis[1])
                .build();
    }
}
