package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import ec.edu.uce.certificadorforense.core.model.imagen.EstructuraImagen;

import java.io.File;
import java.io.FileInputStream;

public class EstructuraImagenService {

    public EstructuraImagen construirAnalisis(File archivo) {
        // 1. Detectar formato real (PNG/JPEG/PSD/DESCONOCIDO)
        String formato = NumerosMagicos.detectarFormatoReal(archivo);

        // 2. Extraer firma hexadecimal para el objeto
        String hex = leerFirmaHex(archivo);

        // 3. Extraer DPIs desde los bytes (pHYs o JFIF)
        int[] dpis = ResolucionFisica.extraerDpi(archivo, formato);

        // 4. CONSTRUIR EL OBJETO USANDO EL BUILDER DE LOMBOK
        return EstructuraImagen.builder()
                .formatoReal(formato)
                .firmaHex(hex)
                .dpiX(dpis[0])
                .dpiY(dpis[1])
                .tieneResolucionFisica(dpis[0] != 72) // Si es diferente a 72, detectó algo físico
                .build();
    }

    private String leerFirmaHex(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] b = new byte[4];
            if (fis.read(b) != -1) {
                return String.format("%02X %02X %02X %02X", b[0], b[1], b[2], b[3]);
            }
        } catch (Exception e) { return "ERROR"; }
        return "00 00 00 00";
    }
}
