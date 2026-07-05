package ec.edu.uce.certificadorforense.core.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CalculadorPHashTest {

    @Test
    public void testImagenesGeneranMismoPHash() throws IOException {
        CalculadorPHash calculador = new CalculadorPHash();
        String directorioPruebas = "/home/edlith/Documentos/UCE 26-26/TESIS/Archivos de Prueba/pruebas_phash";
        File dir = new File(directorioPruebas);
        
        assertTrue(dir.exists() && dir.isDirectory(), "El directorio de pruebas no existe: " + directorioPruebas);

        File[] archivos = dir.listFiles((d, name) -> name.matches(".*\\.(jpg|jpeg|png)$"));
        assertNotNull(archivos, "No se encontraron archivos en el directorio de pruebas");
        assertTrue(archivos.length > 1, "Debe haber al menos 2 imágenes para comparar");

        List<String> hashes = new ArrayList<>();
        List<String> nombres = new ArrayList<>();

        for (File archivo : archivos) {
            BufferedImage imagen = ImageIO.read(archivo);
            assertNotNull(imagen, "No se pudo leer la imagen: " + archivo.getName());
            
            String hash = calculador.generarHash(imagen);
            hashes.add(hash);
            nombres.add(archivo.getName());
            
            System.out.println("Archivo: " + archivo.getName() + " -> pHash: " + hash);
        }

        String primerHash = hashes.get(0);
        for (int i = 1; i < hashes.size(); i++) {
            double similitud = calculador.compararSimilitud(primerHash, hashes.get(i));
            assertTrue(similitud >= 95.0, 
                "El hash de la imagen '" + nombres.get(i) + "' no es suficientemente similar a la primera imagen '" + nombres.get(0) + "'. Similitud: " + similitud + "%");
        }
    }
}
