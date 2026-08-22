package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Servicio de dominio: cálculo de hashes SHA-512.
 * <p>
 * Wrapper de dominio sobre {@link GeneradorHashPort} que proporciona
 * métodos convenientes para los distintos casos de uso.
 * </p>
 */
@ApplicationScoped
public class HashSHA512Service {

    private final GeneradorHashPort generadorHash;

    @Inject
    public HashSHA512Service(GeneradorHashPort generadorHash) {
        this.generadorHash = generadorHash;
    }

    /**
     * Calcula SHA-512 de un arreglo de bytes (archivos).
     *
     * @param bytes Bytes a hashear.
     * @return Hash hexadecimal de 128 caracteres.
     */
    public String calcular(byte[] bytes) {
        return generadorHash.calcularSHA512(bytes);
    }

    /**
     * Calcula SHA-512 de un texto (JSON del expediente, etc.).
     *
     * @param texto Texto a hashear.
     * @return Hash hexadecimal de 128 caracteres.
     */
    public String calcular(String texto) {
        return generadorHash.calcularSHA512(texto);
    }

    /**
     * Calcula SHA-512 de un archivo leyéndolo por bloques (PSD/imagen), sin
     * cargarlo completo en memoria.
     *
     * @param archivo Archivo a hashear.
     * @return Hash hexadecimal de 128 caracteres.
     */
    public String calcular(java.io.File archivo) throws java.io.IOException {
        return generadorHash.calcularSHA512(archivo);
    }
}
