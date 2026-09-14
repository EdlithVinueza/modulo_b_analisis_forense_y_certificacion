package ec.edu.uce.certificadorforense.application.usecase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.ports.in.EmitirCertificadoUseCase;
import ec.edu.uce.certificadorforense.core.ports.out.*;
import ec.edu.uce.certificadorforense.core.service.CertificadoService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class EmitirCertificadoUseCaseImpl implements EmitirCertificadoUseCase {

    @Inject
    ExpedienteRepositoryPort expedienteRepository;

    @Inject
    SelladorInstitucionalPort selladorInstitucional;

    @Inject
    GeneradorHashPort hashPort;

    @Inject
    GeneradorQRPort qrPort;

    @Inject
    GeneradorPDFPort generadorPDF;

    @Inject
    @Named("jpeg")
    InyeccionDatosPort inyeccionJpeg;

    @Inject
    @Named("png")
    InyeccionDatosPort inyeccionPng;

    @ConfigProperty(name = "tesis.cert.password")
    Optional<String> certPassword;

    @Override
    @Transactional
    public byte[] emitir(String idExpediente) throws Exception {
        FirmaAutor firma = expedienteRepository.buscarFirmaPorExpedienteId(idExpediente)
                .orElseThrow(() -> new RuntimeException("No se encontró la firma digital para el expediente: " + idExpediente));

        String expedienteJson = firma.getExpedienteJson();
        if (expedienteJson == null || expedienteJson.isEmpty()) {
            throw new RuntimeException("El JSON del expediente firmado no se encuentra en la base de datos.");
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (com.google.gson.JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
                .create();
        Expediente expediente = gson.fromJson(expedienteJson, Expediente.class);

        CertificadoService certServ = new CertificadoService(qrPort, hashPort);
        String expedienteFirmadoJson = expedienteJson + "\n---FIRMA---\n" + firma.getFirmaBase64();
        Certificado certificado = certServ.generar(expediente, expedienteFirmadoJson);

        byte[] imagenRaw = expedienteRepository.obtenerImagenRaw(idExpediente);
        if (imagenRaw == null || imagenRaw.length == 0) {
            throw new RuntimeException("No se encontró el binario de la imagen para certificar en el expediente: " + idExpediente);
        }

        String imagenBase64 = Base64.getEncoder().encodeToString(imagenRaw);

        byte[] pdfSinFirmar = generadorPDF.generar(certificado, expediente, expedienteFirmadoJson, imagenBase64);
        byte[] pdfFirmado = selladorInstitucional.sellar(pdfSinFirmar, certPassword.orElse(""));

        String ext = "png";
        ExpedienteResumen expDb = expedienteRepository.buscarResumenPorId(idExpediente).orElse(null);
        if (expDb != null && expDb.getEvidenciaTecnicaJson() != null) {
            try {
                JsonObject jsonEv = JsonParser.parseString(expDb.getEvidenciaTecnicaJson()).getAsJsonObject();
                if (jsonEv.has("extensionReal")) {
                    ext = jsonEv.get("extensionReal").getAsString().toLowerCase();
                }
            } catch (Exception ignored) {}
        }

        InyeccionDatosPort inyector = (ext.contains("jpg") || ext.contains("jpeg")) ? inyeccionJpeg : inyeccionPng;
        String jsonInyeccion = "{\"id\":\"" + certificado.getIdCertificado() + "\",\"hash\":\""
                + certificado.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = inyector.inyectar(imagenRaw, jsonInyeccion);

        ByteArrayOutputStream baosZip = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baosZip);

        ZipEntry pdfEntry = new ZipEntry(certificado.getIdCertificado() + ".pdf");
        zos.putNextEntry(pdfEntry);
        zos.write(pdfFirmado);
        zos.closeEntry();

        String imgExt = ext.contains("jpg") || ext.contains("jpeg") ? ".jpg" : ".png";
        ZipEntry imgEntry = new ZipEntry(certificado.getIdCertificado() + "-obra-certificada" + imgExt);
        zos.putNextEntry(imgEntry);
        zos.write(imagenCert);
        zos.closeEntry();
        zos.close();

        byte[] zipGenerado = baosZip.toByteArray();

        expedienteRepository.guardarCertificado(idExpediente, certificado, expedienteFirmadoJson, zipGenerado);
        expedienteRepository.limpiarImagenRaw(idExpediente);

        return zipGenerado;
    }

    @Override
    public boolean esPropietario(String idExpediente, String cedulaSolicitante) {
        if (cedulaSolicitante == null) return false;
        return expedienteRepository.buscarResumenPorId(idExpediente)
                .map(exp -> cedulaSolicitante.equals(exp.getUsuarioCedula()))
                .orElse(false);
    }

    @Override
    public byte[] obtenerZipYLimpiar(String idExpediente) {
        byte[] zip = expedienteRepository.obtenerZipCertificado(idExpediente);
        if (zip == null || zip.length == 0) {
            throw new RuntimeException("El archivo ZIP no se generó correctamente o el expediente no existe: " + idExpediente);
        }
        return zip;
    }
}
