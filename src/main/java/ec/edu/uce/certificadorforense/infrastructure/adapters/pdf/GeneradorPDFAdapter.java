package ec.edu.uce.certificadorforense.infrastructure.adapters.pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorPDFPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Adaptador de infraestructura: generación del PDF visual del certificado Verisart.
 * Utiliza Thymeleaf para procesar una plantilla HTML y html2pdf para la conversión.
 */
@ApplicationScoped
public class GeneradorPDFAdapter implements GeneradorPDFPort {

    private final TemplateEngine templateEngine;

    public GeneradorPDFAdapter() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    private String wrapHash(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            if ((i + 1) % 16 == 0 && i != input.length() - 1) {
                sb.append("\u200B"); // Zero-width space
            }
        }
        return sb.toString();
    }

    @Override
    public byte[] generar(Certificado certificado, Expediente expediente,
                          String expedienteJson, String imagenBase64) {
        
        Context ctx = new Context();
        ctx.setVariable("idCertificado", certificado.getIdCertificado());
        String fechaStr = certificado.getFechaEmision() != null
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC).format(certificado.getFechaEmision())
                : "";
        ctx.setVariable("fechaEmision", fechaStr);
        ctx.setVariable("versionMetadatos", "1.1");
        
        String nombresRaw = expediente != null && expediente.getAutor() != null ? expediente.getAutor().getNombres() : "";
        String apellidosRaw = expediente != null && expediente.getAutor() != null ? expediente.getAutor().getApellidos() : "";
        
        ec.edu.uce.certificadorforense.infrastructure.adapters.security.VaultEncryptionService vaultService = 
                new ec.edu.uce.certificadorforense.infrastructure.adapters.security.VaultEncryptionService();
        String nombres = vaultService.decrypt(nombresRaw);
        String apellidos = vaultService.decrypt(apellidosRaw);
        String autorNombreCompleto = (nombres + " " + apellidos).trim();
        if (autorNombreCompleto.isEmpty() && expediente != null && expediente.getAutor() != null) {
            autorNombreCompleto = expediente.getAutor().getNombreCompleto();
        }

        ctx.setVariable("autorObra", autorNombreCompleto);
        ctx.setVariable("seudonimo", expediente != null && expediente.getAutor() != null ? expediente.getAutor().getSeudonimo() : "");
        ctx.setVariable("idInstitucional", expediente != null && expediente.getAutor() != null ? expediente.getAutor().getCedula() : "");
        
        ctx.setVariable("tituloObra", expediente != null && expediente.getObra() != null ? expediente.getObra().getTitulo() : "");
        ctx.setVariable("descripcionObra", expediente != null && expediente.getObra() != null ? expediente.getObra().getDescripcion() : "");
        ctx.setVariable("categoriaObra", expediente != null && expediente.getObra() != null && expediente.getObra().getCategoria() != null ? expediente.getObra().getCategoria().getEtiqueta() : "");
        ctx.setVariable("fechaCreacion", expediente != null && expediente.getObra() != null && expediente.getObra().getFechaCreacion() != null ? expediente.getObra().getFechaCreacion().toString() : "");
        ctx.setVariable("software", expediente != null && expediente.getObra() != null ? expediente.getObra().getSoftware() : "");
        ctx.setVariable("hardware", expediente != null && expediente.getObra() != null && expediente.getObra().getHardware() != null ? expediente.getObra().getHardware() : "");
        ctx.setVariable("detallesTecnicos", expediente != null && expediente.getAnalisis() != null ? expediente.getAnalisis().getDetallesTecnicos() : "");
        
        ctx.setVariable("capasPSD", expediente != null && expediente.getAnalisis() != null ? expediente.getAnalisis().getCapasPSD() : 0);
        ctx.setVariable("metadatosDetectados", expediente != null && expediente.getAnalisis() != null && expediente.getAnalisis().isMetadatosDetectados());
        ctx.setVariable("dimensiones", expediente != null && expediente.getAnalisis() != null ? expediente.getAnalisis().getDimensiones() : "");
        
        // Inyectamos espacios invisibles (Zero-width space) cada 16 caracteres para forzar a iText a romper la línea
        ctx.setVariable("sha512PSD", wrapHash(expediente != null && expediente.getHashes() != null ? expediente.getHashes().getSha512PSD() : ""));
        ctx.setVariable("sha512Imagen", wrapHash(expediente != null && expediente.getHashes() != null ? expediente.getHashes().getSha512Imagen() : ""));
        ctx.setVariable("phash", wrapHash(expediente != null && expediente.getHashes() != null ? expediente.getHashes().getPHash() : ""));
        ctx.setVariable("estado", expediente != null && expediente.getAnalisis() != null ? expediente.getAnalisis().getResultado() : "VÁLIDO");
        
        ctx.setVariable("obraBase64", imagenBase64);

        // Garantizar que el QR no sea null
        String qrBase64 = certificado.getQrBase64();
        if (qrBase64 == null || qrBase64.trim().isEmpty()) {
            try {
                ec.edu.uce.certificadorforense.infrastructure.adapters.qr.QRGeneratorAdapter qrGen = 
                        new ec.edu.uce.certificadorforense.infrastructure.adapters.qr.QRGeneratorAdapter();
                String qrText = certificado.getQrContenido() != null ? certificado.getQrContenido() : certificado.getIdCertificado();
                byte[] qrBytes = qrGen.generar(qrText, 150, 150);
                qrBase64 = java.util.Base64.getEncoder().encodeToString(qrBytes);
            } catch (Exception e) {
                qrBase64 = "";
            }
        }
        ctx.setVariable("qrBase64", qrBase64);

        String html = templateEngine.process("certificado", ctx);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4.rotate());

            // Adjuntar expediente JSON antes de convertir HTML
            PdfFileSpec adjunto = PdfFileSpec.createEmbeddedFileSpec(
                    pdf,
                    expedienteJson.getBytes(StandardCharsets.UTF_8),
                    "Expediente Firmado Verisart",
                    "expediente-firmado.json",
                    null,
                    new PdfName("application/json")
            );
            pdf.addFileAttachment("expediente-firmado.json", adjunto);

            // Metadata XMP
            PdfDocumentInfo info = pdf.getDocumentInfo();
            info.setTitle("Certificado Verisart — " + certificado.getIdCertificado());
            info.setSubject("Certificado de Autenticidad Digital");
            info.setKeywords("idCertificado=" + certificado.getIdCertificado()
                    + "; idExpediente=" + certificado.getIdExpediente()
                    + "; hash=" + certificado.getHashExpedienteFirmado());
            info.setCreator("Sistema Verisart — UCE");

            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(html, pdf, properties);

            if (!pdf.isClosed()) {
                pdf.close();
            }

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("[GeneradorPDFAdapter] Error generando PDF con HTML: " + e.getMessage(), e);
        }
    }
}
