package ec.edu.uce.certificadorforense.core.service;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CalculadorPHash {

    public String generarHash(BufferedImage imagen) {
        if (imagen == null) {
            return "0".repeat(64);
        }
        // 1. Redimensionar a 8x8 para normalizar
        Image escala = imagen.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage miniatura = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);

        Graphics g = miniatura.getGraphics();
        g.drawImage(escala, 0, 0, null);
        g.dispose();

        // 2. Calcular brillo promedio
        double sumaGris = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                sumaGris += (miniatura.getRGB(x, y) & 0xFF);
            }
        }
        double promedio = sumaGris / 64.0;

        // 3. Generar cadena binaria de 64 bits
        StringBuilder hash = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                hash.append((miniatura.getRGB(x, y) & 0xFF) >= promedio ? "1" : "0");
            }
        }
        return hash.toString();
    }

    public double compararSimilitud(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length()) {
            return 0.0;
        }
        int distanciaHamming = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distanciaHamming++;
            }
        }
        // Retorna el porcentaje de parecido
        return (1.0 - (distanciaHamming / 64.0)) * 100.0;
    }
}