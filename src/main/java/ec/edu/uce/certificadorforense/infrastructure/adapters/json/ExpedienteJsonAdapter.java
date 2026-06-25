package ec.edu.uce.certificadorforense.infrastructure.adapters.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;

/**
 * Adaptador de infraestructura: persistencia del expediente en archivo JSON.
 * <p>
 * <strong>Implementación temporal</strong> — cuando se elija el framework REST
 * (Spring Boot o Quarkus), este adaptador se reemplaza por uno PostgreSQL
 * sin modificar ninguna clase del {@code core}.
 * </p>
 *
 * <p>Los archivos se guardan en {@code resultados certificacion/expedientes/}.</p>
 */
public class ExpedienteJsonAdapter implements ExpedienteRepositoryPort {

    //hasta migrar a una base de datos 
    private static final String DEFAULT_DIRECTORIO = "resultados_test_certificacion/expedientes";
    private final String directorio;
    private final Gson gson;

    public ExpedienteJsonAdapter() {
        this(DEFAULT_DIRECTORIO);
    }

    public ExpedienteJsonAdapter(String directorio) {
        this.directorio = directorio;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void guardar(Expediente expediente, String expedienteJson, String firmaBase64) {
        try {
            Path dir = Paths.get(this.directorio);
            Files.createDirectories(dir);

            // Guardar expediente base (JSON del dominio)
            String nombreBase = expediente.getIdExpediente() + ".json";
            Path rutaBase = dir.resolve(nombreBase);
            Files.writeString(rutaBase, expedienteJson, StandardCharsets.UTF_8);

            // Guardar también el wrapper con la firma
            String nombreFirmado = expediente.getIdExpediente() + "-firmado.json";
            Path rutaFirmada = dir.resolve(nombreFirmado);
            String wrapperJson = gson.toJson(new ExpedienteFirmadoWrapper(expedienteJson, firmaBase64));
            Files.writeString(rutaFirmada, wrapperJson, StandardCharsets.UTF_8);

            System.out.println("[ExpedienteJsonAdapter] Expediente guardado: " + rutaFirmada.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Error guardando expediente: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Expediente> buscarPorId(String idExpediente) {
        try {
            Path ruta = Paths.get(this.directorio, idExpediente + ".json");
            if (!Files.exists(ruta)) {
                return Optional.empty();
            }
            String json = Files.readString(ruta, StandardCharsets.UTF_8);
            return Optional.of(gson.fromJson(json, Expediente.class));
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo expediente: " + e.getMessage(), e);
        }
    }

    /** Wrapper interno para el archivo firmado (expediente + firma). */
    private record ExpedienteFirmadoWrapper(String expedienteJson, String firmaBase64) {}
}
