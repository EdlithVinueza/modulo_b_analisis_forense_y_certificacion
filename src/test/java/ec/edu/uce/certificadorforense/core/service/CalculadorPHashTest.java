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
        String directorioPruebas = "C:/Users/edlit/OneDrive/Documentos/TESIS/Archivos de Prueba/pruebas_phash";
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
            
            // Imprimir el resultado de la similitud en la consola
            System.out.println("Comparando '" + nombres.get(0) + "' con '" + nombres.get(i) + "' -> Similitud: " + String.format("%.2f", similitud) + "%");
            
            assertTrue(similitud >= 95.0, 
                "El hash de la imagen '" + nombres.get(i) + "' no es suficientemente similar a la primera imagen '" + nombres.get(0) + "'. Similitud: " + similitud + "%");
        }
    }

    @Test
    public void testImagenesDistintasGeneranPHashDiferente() {
        CalculadorPHash calculador = new CalculadorPHash();

        // 1. Crear imagen A (diagonal blanca en fondo negro)
        BufferedImage imgA = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 64; i++) {
            imgA.setRGB(i, i, 0xFFFFFF);
        }

        // 2. Crear imagen B (cuadro blanco centrado en fondo negro)
        BufferedImage imgB = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int y = 20; y < 44; y++) {
            for (int x = 20; x < 44; x++) {
                imgB.setRGB(x, y, 0xFFFFFF);
            }
        }

        String hashA = calculador.generarHash(imgA);
        String hashB = calculador.generarHash(imgB);

        double similitud = calculador.compararSimilitud(hashA, hashB);
        System.out.println("Prueba de imágenes totalmente distintas -> Similitud: " + String.format("%.2f", similitud) + "%");

        assertTrue(similitud < 85.0, "Imágenes estructuralmente distintas no deben ser parecidas (>85%). Obtenido: " + similitud + "%");
    }
}
