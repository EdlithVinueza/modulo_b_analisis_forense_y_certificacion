package ec.edu.uce.certificadorforense;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.AnalisisResumen;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.expediente.HashesEvidencia;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.infrastructure.adapters.pdf.GeneradorPDFAdapter;
import ec.edu.uce.certificadorforense.infrastructure.adapters.qr.QRGeneratorAdapter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneradorPDFTest {

    @Test
    public void testGenerarPdfConDatosMock() throws Exception {
        // 1. Mock Autor (Nombre claro no encriptado)
        Autor autor = Autor.builder()
                .nombres("GONZALO LUIS")
                .apellidos("BALCAZAR CAMPOVERDE")
                .cedula("9900000003")
                .correo("gonzalo@ejemplo.com")
                .seudonimo("artGon")
                .build();

        // 2. Mock Obra
        Obra obra = Obra.builder()
                .titulo("Girasoles del Amanecer")
                .descripcion("Obra digital original creada para titulación")
                .software("Adobe Photoshop 2026")
                .hardware("Tableta Gráfica Wacom Cintiq")
                .categoria(CategoriaObra.ILUSTRACION)
                .fechaCreacion(LocalDate.of(2026, 6, 15))
                .build();

        // 3. Mock Análisis Forense
        AnalisisResumen analisis = AnalisisResumen.builder()
                .resultado("APROBADO")
                .capasPSD(12)
                .metadatosDetectados(true)
                .dimensiones("4320 x 5400 px")
                .detallesTecnicos("Ilustración, 600 DPI, RGB")
                .build();

        // 4. Mock Hashes
        HashesEvidencia hashes = HashesEvidencia.builder()
                .sha512PSD("0e971e9a0b2c3ef315de5cd456c4109de9ca11f0c02234cff208fd2b9af5bce4bbab3bfa1153f6547ca81314d2adb348b498db07482e78ef12d9951381331a16")
                .sha512Imagen("0e971e9a0b2c3ef315de5cd456c4109de9ca11f0c02234cff208fd2b9af5bce4bbab3bfa1153f6547ca81314d2adb348b498db07482e78ef12d9951381331a16")
                .pHash("00000001001100110011001111110010111001001110100011010000010000")
                .build();

        // 5. Mock Expediente
        Expediente expediente = Expediente.builder()
                .idExpediente("EXP-2026-000001")
                .fechaRegistro(Instant.now().toString())
                .autor(autor)
                .obra(obra)
                .analisis(analisis)
                .hashes(hashes)
                .build();

        // 6. QR Code Generation
        QRGeneratorAdapter qrAdapter = new QRGeneratorAdapter();
        String qrContent = "Certificado: CERT-2026-000001\nExpediente: EXP-2026-000001\nObra: Girasoles del Amanecer\nAutor: GONZALO LUIS BALCAZAR CAMPOVERDE";
        byte[] qrBytes = qrAdapter.generar(qrContent, 150, 150);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);

        // 7. Mock Certificado
        Certificado certificado = Certificado.builder()
                .idCertificado("CERT-2026-000001")
                .idExpediente("EXP-2026-000001")
                .fechaEmision(Instant.now())
                .hashExpedienteFirmado("0e971e9a0b2c3ef315de5cd456c4109de9ca11f0c02234cff208fd2b9af5bce4bbab3bfa1153f6547ca81314d2adb348b498db07482e78ef12d9951381331a16")
                .qrContenido(qrContent)
                .qrBase64(qrBase64)
                .build();

        // 8. Load image preview if available
        File imgFile = new File("c:\\Users\\edlit\\OneDrive\\Documentos\\TESIS\\Codigo\\pruebas\\girasol-original.png");
        String imgBase64 = "";
        if (imgFile.exists()) {
            byte[] imgBytes = Files.readAllBytes(imgFile.toPath());
            imgBase64 = Base64.getEncoder().encodeToString(imgBytes);
        }

        // 9. Render PDF
        GeneradorPDFAdapter generadorPDF = new GeneradorPDFAdapter();
        byte[] pdfBytes = generadorPDF.generar(certificado, expediente, "{\"id\":\"EXP-2026-000001\"}", imgBase64);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Save PDF artifact for inspection
        File outputFile = new File("c:\\Users\\edlit\\OneDrive\\Documentos\\TESIS\\Codigo\\pruebas\\test_certificado_mock.pdf");
        Files.write(outputFile.toPath(), pdfBytes);
        System.out.println("[GeneradorPDFTest] PDF de prueba generado exitosamente en: " + outputFile.getAbsolutePath());
    }
}
