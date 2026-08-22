package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.RecuperacionDatos;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorPDFPort;
import ec.edu.uce.certificadorforense.core.ports.out.InyeccionDatosPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

@ApplicationScoped
public class RecuperacionCertificadoService {

    @jakarta.inject.Inject
    ExpedienteRepositoryPort expedienteRepository;

    @jakarta.inject.Inject
    GeneradorPDFPort generadorPDF;

    /** Inyector de datos para JPEG — implementado por InyeccionDatosJPEGAdapter (@Named("jpeg")). */
    @jakarta.inject.Inject
    @jakarta.inject.Named("jpeg")
    InyeccionDatosPort inyeccionJpeg;

    /** Inyector de datos para PNG — implementado por InyeccionDatosPNGAdapter (@Named("png")). */
    @jakarta.inject.Inject
    @jakarta.inject.Named("png")
    InyeccionDatosPort inyeccionPng;

    @Transactional
    public byte[] recuperarCertificadoLocalmente(String hashImagen, String cedula, File imgFile, String extension) throws Exception {
        RecuperacionDatos datos = expedienteRepository.buscarParaRecuperacion(hashImagen)
                .orElseThrow(() -> new RuntimeException(
                        "Error: No se encontró la obra duplicada en la base de datos, o no tiene un certificado emitido."));

        if (!datos.getUsuarioCedula().equals(cedula)) {
            // ALERTA DE FRAUDE
            throw new RuntimeException("ConflictoPropiedadException: Esta obra ya se encuentra registrada a nombre de otro autor. Posible intento de plagio.");
        }

        Certificado certModelo = datos.getCertificado();
        String expedienteFirmadoRaw = datos.getExpedienteFirmadoRaw();

        // Recuperar y reensamblar (OPCIÓN B)
        byte[] imagenRaw = Files.readAllBytes(imgFile.toPath());
        String imagenBase64 = Base64.getEncoder().encodeToString(imagenRaw);

        // No tenemos el objeto Expediente completo deserializado fácilmente, pero el GeneradorPDFAdapter actual
        // requiere el objeto Expediente. Haremos un truco: Deserializamos el expedienteFirmadoRaw a JsonElement
        // o adaptamos GeneradorPDFAdapter.
        // Dado que solo queremos que la tesis funcione:
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(java.time.LocalDate.class, (com.google.gson.JsonDeserializer<java.time.LocalDate>) (json, typeOfT, context) -> java.time.LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
                .create();
        String jsonPuro = expedienteFirmadoRaw.split("\n---FIRMA---\n")[0];
        ec.edu.uce.certificadorforense.core.model.expediente.Expediente expedienteOriginal =
                gson.fromJson(jsonPuro, ec.edu.uce.certificadorforense.core.model.expediente.Expediente.class);

        byte[] pdfGenerado = generadorPDF.generar(certModelo, expedienteOriginal, expedienteFirmadoRaw, imagenBase64);

        // Inyección de Datos
        InyeccionDatosPort inyector = (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg"))
                ? inyeccionJpeg : inyeccionPng;

        String jsonInyeccion = "{\"id\":\"" + certModelo.getIdCertificado() + "\",\"hash\":\"" + certModelo.getHashExpedienteFirmado() + "\"}";
        byte[] imagenCert = inyector.inyectar(imagenRaw, jsonInyeccion);

        // Generar Zip
        java.io.ByteArrayOutputStream baosZip = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baosZip);

        java.util.zip.ZipEntry pdfEntry = new java.util.zip.ZipEntry(certModelo.getIdCertificado() + "-RECUPERADO.pdf");
        zos.putNextEntry(pdfEntry);
        zos.write(pdfGenerado);
        zos.closeEntry();

        java.util.zip.ZipEntry imgEntry = new java.util.zip.ZipEntry(certModelo.getIdCertificado() + "-obra-certificada." + extension);
        zos.putNextEntry(imgEntry);
        zos.write(imagenCert);
        zos.closeEntry();
        zos.close();

        return baosZip.toByteArray();
    }
}
