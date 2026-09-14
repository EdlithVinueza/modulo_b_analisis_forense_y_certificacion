package ec.edu.uce.certificadorforense.application.usecase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.ports.in.EmitirCertificadoUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.FirmarExpedienteUseCase;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.core.ports.out.FirmadorNubePort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.security.VaultEncryptionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@ApplicationScoped
public class FirmarExpedienteUseCaseImpl implements FirmarExpedienteUseCase {

    @Inject
    ExpedienteRepositoryPort expedienteRepository;

    @Inject
    FirmadorNubePort firmadorNube;

    @Inject
    VaultEncryptionService vaultEncryptionService;

    @Inject
    EmitirCertificadoUseCase emitirCertificadoUseCase;

    @Override
    @Transactional
    public String ejecutar(String idExpediente, String password) throws Exception {
        ExpedienteResumen expDb = expedienteRepository.buscarResumenPorId(idExpediente)
                .orElseThrow(() -> new RuntimeException("Expediente no encontrado en BD."));

        Autor autor = Autor.builder()
                .nombres(expDb.getUsuarioNombres() != null ? expDb.getUsuarioNombres() : "")
                .apellidos(expDb.getUsuarioApellidos() != null ? expDb.getUsuarioApellidos() : "")
                .cedula(expDb.getUsuarioCedula() != null ? expDb.getUsuarioCedula() : "")
                .correo(expDb.getUsuarioCorreo() != null ? expDb.getUsuarioCorreo() : "")
                .seudonimo(expDb.getUsuarioNombreArtistico() != null ? expDb.getUsuarioNombreArtistico() : "")
                .build();

        CategoriaObra cat = CategoriaObra.ILUSTRACION;
        if (expDb.getObraCategoria() != null) {
            for (CategoriaObra c : CategoriaObra.values()) {
                if (c.name().equalsIgnoreCase(expDb.getObraCategoria())) {
                    cat = c;
                    break;
                }
            }
        }

        Obra obra = Obra.builder()
                .titulo(expDb.getObraTitulo())
                .descripcion(expDb.getObraDescripcion())
                .software(expDb.getObraSoftware())
                .hardware(expDb.getObraHardware())
                .categoria(cat)
                .fechaCreacion(expDb.getObraFechaCreacion() != null ? expDb.getObraFechaCreacion() : LocalDate.now())
                .build();

        int ancho = 0;
        int alto = 0;
        int capas = 0;
        boolean metadatos = true;
        double dpi = 72.0;
        if (expDb.getEvidenciaTecnicaJson() != null && !expDb.getEvidenciaTecnicaJson().isEmpty()) {
            try {
                com.google.gson.JsonObject jsonEv = com.google.gson.JsonParser.parseString(expDb.getEvidenciaTecnicaJson()).getAsJsonObject();
                if (jsonEv.has("ancho")) ancho = jsonEv.get("ancho").getAsInt();
                if (jsonEv.has("alto")) alto = jsonEv.get("alto").getAsInt();
                if (jsonEv.has("capasPSD")) capas = jsonEv.get("capasPSD").getAsInt();
                if (jsonEv.has("metadatosDetectados")) metadatos = jsonEv.get("metadatosDetectados").getAsBoolean();
                if (jsonEv.has("dpi")) dpi = jsonEv.get("dpi").getAsDouble();
            } catch (Exception ignored) {}
        }

        String dimensionesStr = (ancho > 0 && alto > 0) ? ancho + " x " + alto + " px" : "Desconocido";
        String detallesTecnicosStr = cat.getEtiqueta() + ", " + String.format("%.0f", dpi) + " DPI";

        ec.edu.uce.certificadorforense.core.model.expediente.AnalisisResumen analisis =
                ec.edu.uce.certificadorforense.core.model.expediente.AnalisisResumen.builder()
                        .resultado("APROBADO")
                        .capasPSD(capas)
                        .metadatosDetectados(metadatos)
                        .dimensiones(dimensionesStr)
                        .detallesTecnicos(detallesTecnicosStr)
                        .build();

        ec.edu.uce.certificadorforense.core.model.expediente.HashesEvidencia hashes =
                ec.edu.uce.certificadorforense.core.model.expediente.HashesEvidencia.builder()
                        .sha512PSD(expDb.getHashPsdOriginal())
                        .sha512Imagen(expDb.getHashImagenFinal())
                        .pHash(expDb.getPhashImagenString())
                        .build();

        int anio = java.time.Year.now().getValue();
        long timestamp = System.currentTimeMillis() % 1000000;
        int random = (int) (Math.random() * 900 + 100);
        String idExp = String.format("EXP-%d-%06d%03d", anio, timestamp, random);

        Expediente expediente = Expediente.builder()
                .idExpediente(idExp)
                .fechaRegistro(Instant.now().toString())
                .autor(autor)
                .obra(obra)
                .analisis(analisis)
                .hashes(hashes)
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (com.google.gson.JsonSerializer<LocalDate>) (src, typeOfSrc,
                                ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc,
                                ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
                .setPrettyPrinting()
                .create();
        String expedienteJson = gson.toJson(expediente);

        String p12Base64 = expDb.getUsuarioFirmaP12();
        if (p12Base64 == null || p12Base64.isEmpty()) {
            throw new RuntimeException("El usuario no tiene una firma digital configurada en el sistema.");
        }

        if (vaultEncryptionService != null) {
            p12Base64 = vaultEncryptionService.decrypt(p12Base64);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(expedienteJson.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashObra = sb.toString();

        String firmaBase64;
        try {
            firmaBase64 = firmadorNube.firmar(p12Base64, password, hashObra);
        } catch (FirmadorNubePort.FirmaNubeException fne) {
            throw new RuntimeException("Error de firma digital en la nube: " + fne.getMessage(), fne);
        } catch (jakarta.ws.rs.WebApplicationException we) {
            we.printStackTrace();
            String responseBody = we.getResponse().readEntity(String.class);
            throw new RuntimeException("Respuesta de la CA: " + responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo completar la firma digital en este momento. Intenta nuevamente en unos minutos.");
        }

        FirmaAutor firma = FirmaAutor.builder()
                .firmaBase64(firmaBase64)
                .hashExpediente(hashObra)
                .algoritmo("SHA512withRSA")
                .fechaFirma(Instant.now())
                .aliasKeystore("AzureCloud")
                .expedienteJson(expedienteJson)
                .build();

        expedienteRepository.guardarFirma(idExpediente, firma, expedienteJson);

        emitirCertificadoUseCase.emitir(idExpediente);

        return firma.getHashExpediente();
    }
}
