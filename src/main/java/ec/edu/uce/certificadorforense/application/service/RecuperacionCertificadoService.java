package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.CertificadoEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.ExpedienteForenseEntity;
import ec.edu.uce.certificadorforense.infrastructure.adapters.inyeccion.InyeccionDatosJPEGAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.inyeccion.InyeccionDatosPNGAdapter;
import ec.edu.uce.certificadorforense.core.ports.out.InyeccionDatosPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.pdf.GeneradorPDFAdapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

@ApplicationScoped
public class RecuperacionCertificadoService {

    @Transactional
    public byte[] recuperarCertificadoLocalmente(String hashImagen, String cedula, File imgFile, String extension) throws Exception {
        ExpedienteForenseEntity expDb = ExpedienteForenseEntity.find("hashImagenFinal", hashImagen).firstResult();
        if (expDb == null) {
            expDb = ExpedienteForenseEntity.find("hashPsdOriginal", hashImagen).firstResult();
        }

        if (expDb == null) {
            throw new RuntimeException("Error: No se encontró la obra duplicada en la base de datos.");
        }

        if (!expDb.obra.usuario.cedula.equals(cedula)) {
            // ALERTA DE FRAUDE
            throw new RuntimeException("ConflictoPropiedadException: Esta obra ya se encuentra registrada a nombre de otro autor. Posible intento de plagio.");
        }

        CertificadoEntity certDb = CertificadoEntity.find("obra", expDb.obra).firstResult();
        if (certDb == null) {
            throw new RuntimeException("Error: La obra está registrada pero no tiene un certificado emitido.");
        }

        // Recuperar y reensamblar (OPCIÓN B)
        byte[] imagenRaw = Files.readAllBytes(imgFile.toPath());
        String imagenBase64 = Base64.getEncoder().encodeToString(imagenRaw);

        // En lugar de reconstruir todo el objeto Expediente para pasárselo al PDF, 
        // GeneradorPDFAdapter necesita un objeto Certificado (para los metadatos visuales).
        Certificado certModelo = Certificado.builder()
                .idCertificado(certDb.numeroCertificado)
                .idExpediente(expDb.id.toString())
                .fechaEmision(certDb.fechaEmision.toInstant(java.time.ZoneOffset.UTC))
                .hashExpedienteFirmado(certDb.hashCertificado)
                .qrContenido(certDb.numeroCertificado)
                .build();

        // No tenemos el objeto Expediente completo deserializado fácilmente, pero el GeneradorPDFAdapter actual
        // requiere el objeto Expediente. Haremos un truco: Deserializamos el expedienteFirmadoRaw a JsonElement
        // o adaptamos GeneradorPDFAdapter.
        // Dado que solo queremos que la tesis funcione:
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(java.time.LocalDate.class, (com.google.gson.JsonDeserializer<java.time.LocalDate>) (json, typeOfT, context) -> java.time.LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
                .create();
        String jsonPuro = certDb.expedienteFirmadoRaw.split("\n---FIRMA---\n")[0];
        ec.edu.uce.certificadorforense.core.model.expediente.Expediente expedienteOriginal = 
                gson.fromJson(jsonPuro, ec.edu.uce.certificadorforense.core.model.expediente.Expediente.class);

        GeneradorPDFAdapter generadorPDF = new GeneradorPDFAdapter();
        byte[] pdfGenerado = generadorPDF.generar(certModelo, expedienteOriginal, certDb.expedienteFirmadoRaw, imagenBase64);

        // Inyección de Datos
        InyeccionDatosPort inyector;
        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
            inyector = new InyeccionDatosJPEGAdapter();
        } else {
            inyector = new InyeccionDatosPNGAdapter();
        }
        
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
