package ec.edu.uce.certificadorforense.infrastructure.adapters.processors;

import java.io.*;

public class ResolucionFisica {

    public static int[] extraerDpi(File f, String formato) {
        int[] dpis = {72, 72}; // Valor por defecto

        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            if ("PNG".equals(formato)) {
                dpis = buscarDpiPng(dis);
            } else if ("JPEG".equals(formato)) {
                dpis = buscarDpiJpeg(dis);
            }
        } catch (Exception ignored) {}
        return dpis;
    }

    private static int[] buscarDpiPng(DataInputStream dis) throws IOException {
        dis.skipBytes(8); // Firma
        while (dis.available() > 0) {
            int length = dis.readInt();
            byte[] type = new byte[4];
            dis.readFully(type);
            if ("pHYs".equals(new String(type))) {
                int x = dis.readInt();
                int y = dis.readInt();
                if (dis.readByte() == 1) { // Unidad: Metros
                    return new int[]{ (int)Math.round(x * 0.0254), (int)Math.round(y * 0.0254) };
                }
                break;
            }
            dis.skipBytes(length + 4); // Datos + CRC
        }
        return new int[]{72, 72};
    }

    private static int[] buscarDpiJpeg(DataInputStream dis) throws IOException {
        dis.skipBytes(2); // FFD8
        while (dis.available() > 0) {
            int marker = dis.readUnsignedShort();
            int length = dis.readUnsignedShort();
            if (marker == 0xFFE0) { // Segmento APP0
                dis.skipBytes(7); // "JFIF\0" + Versión
                int units = dis.readUnsignedByte();
                int x = dis.readUnsignedShort();
                int y = dis.readUnsignedShort();
                if (units == 1) return new int[]{x, y}; // Pulgadas
                if (units == 2) return new int[]{(int)(x * 2.54), (int)(y * 2.54)}; // cm
                break;
            }
            dis.skipBytes(length - 2);
        }
        return new int[]{72, 72};
    }
}
