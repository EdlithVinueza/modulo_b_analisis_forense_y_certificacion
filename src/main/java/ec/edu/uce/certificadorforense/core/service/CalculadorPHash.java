package ec.edu.uce.certificadorforense.core.service;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CalculadorPHash {

    public String generarHash(BufferedImage imagen) {
        if (imagen == null) {
            return "0".repeat(64);
        }

        // 1. Quitar transparencia (Canal Alpha) pintando sobre un lienzo blanco ANTES de escalar.
        // Si escalamos primero, los píxeles transparentes (usualmente rgba(0,0,0,0)) promedian
        // sus colores negros con los píxeles adyacentes, oscureciendo la miniatura.
        BufferedImage sinTransparencia = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = sinTransparencia.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, sinTransparencia.getWidth(), sinTransparencia.getHeight());
        g1.drawImage(imagen, 0, 0, null);
        g1.dispose();

        // 2. Redimensionar a 8x8 para normalizar
        Image escala = sinTransparencia.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage miniatura = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2 = miniatura.createGraphics();
        g2.drawImage(escala, 0, 0, null);
        g2.dispose();

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