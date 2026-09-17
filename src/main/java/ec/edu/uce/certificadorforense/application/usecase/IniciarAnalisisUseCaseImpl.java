package ec.edu.uce.certificadorforense.application.usecase;

import ec.edu.uce.certificadorforense.application.cache.ProcesoCacheManager;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.modelimplement.ArchivoBase;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import ec.edu.uce.certificadorforense.core.ports.in.IniciarAnalisisUseCase;
import ec.edu.uce.certificadorforense.core.ports.out.ArchivoProcessorPort;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.core.ports.out.GeneradorHashPort;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaAnalisisOrigen;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaCoherenciaDpi;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.imagen.ReglaFirmaEstructural;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaComplejidadDiseno;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaFormatoPsd;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaImagenPegada;
import ec.edu.uce.certificadorforense.core.rules.rulesimplement.psd.ReglaResolucionProfesional;
import ec.edu.uce.certificadorforense.core.service.ArchivoProcessorFactory;
import ec.edu.uce.certificadorforense.core.service.CalculadorPHash;
import ec.edu.uce.certificadorforense.core.service.ValidadorGenericoService;
import ec.edu.uce.certificadorforense.core.state.stateimplement.AnalisisForenseState;
import ec.edu.uce.certificadorforense.core.state.stateimplement.ContextoProceso;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ArchivoImagenProcessor;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ArchivoPSDProcessor;
import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ImageLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

@ApplicationScoped
public class IniciarAnalisisUseCaseImpl implements IniciarAnalisisUseCase {

    @Inject
    ExpedienteRepositoryPort expedienteRepository;

    @Inject
    GeneradorHashPort hashPort;

    @Inject
    ProcesoCacheManager cacheManager;

    private static boolean esDuplicadoBloqueante(ExpedienteResumen exp) {
        return exp != null && ("CERTIFICADO".equals(exp.getEstadoActual()) || "FINALIZADO".equals(exp.getEstadoActual()));
    }

    @Override
    public Map<String, String> ejecutar(File psdFile, File imgFile, String extension) throws Exception {
        return ejecutar(psdFile, imgFile, extension, null);
    }

    @Override
    public Map<String, String> ejecutar(File psdFile, File imgFile, String extension, UUID usuarioId) throws Exception {
        List<ArchivoProcessorPort<? extends ArchivoBase>> procesadores = Arrays.asList(
                new ArchivoImagenProcessor(),
                new ArchivoPSDProcessor());
        ArchivoProcessorFactory factory = new ArchivoProcessorFactory(procesadores);

        ValidadorGenericoService<ArchivoImagen> validadorImagen = new ValidadorGenericoService<>();
        validadorImagen.registrarRegla(new ReglaFirmaEstructural());
        validadorImagen.registrarRegla(new ReglaCoherenciaDpi());
        validadorImagen.registrarRegla(new ReglaAnalisisOrigen());

        ValidadorGenericoService<ArchivoPSD> validadorPSD = new ValidadorGenericoService<>();
        validadorPSD.registrarRegla(new ReglaFormatoPsd());
        validadorPSD.registrarRegla(new ReglaResolucionProfesional());
        validadorPSD.registrarRegla(new ReglaImagenPegada());
        validadorPSD.registrarRegla(new ReglaComplejidadDiseno());

        CalculadorPHash calcPHash = new CalculadorPHash();

        ContextoProceso contexto = new ContextoProceso(new AnalisisForenseState());
        String expedienteId = UUID.randomUUID().toString();

        try {
            // 1. Calcular Hash de la Imagen y procesarla primero (Early Return)
            String sha512Imagen = hashPort.calcularSHA512(imgFile);
            contexto.setSha512Imagen(sha512Imagen);

            @SuppressWarnings("unchecked")
            ArchivoImagen imagen = ((ArchivoProcessorPort<ArchivoImagen>) factory.getProcessor(imgFile))
                    .procesar(imgFile);
            contexto.setArchivoImagen(imagen);
            contexto.setImagenRaw(Files.readAllBytes(imgFile.toPath()));

            VeredictoFinal veredictoImagen = validadorImagen.validar(imagen);
            if (veredictoImagen.isEsRechazado()) {
                throw new RuntimeException("Rechazo Imagen: " + veredictoImagen.getRazonRechazo());
            }
            contexto.setVeredictoImagen(veredictoImagen);

            // Calcular pHash de la imagen
            BufferedImage imgImagen = ImageLoader.loadWithSubsampling(imgFile);
            String pHashStr = calcPHash.generarHash(imgImagen);
            if (imgImagen != null) {
                imgImagen.flush();
            }
            contexto.setPHash(pHashStr);

            // Verificar Duplicados tempranamente por el hash exacto de la Imagen
            ExpedienteResumen expAnterior = expedienteRepository.buscarPorHashImagen(contexto.getSha512Imagen())
                    .orElse(null);

            if (expAnterior == null) {
                // Verificar por similitud visual (pHash cruzando formatos)
                List<ExpedienteResumen> candidatos = expedienteRepository.listarCertificadosConPHash();
                for (ExpedienteResumen dbExp : candidatos) {
                    double pHashSimilitud = calcPHash.compararSimilitud(pHashStr, dbExp.getPhashImagenString());
                    if (pHashSimilitud >= 95.0) {
                        try {
                            com.google.gson.JsonObject jsonEvidencia = com.google.gson.JsonParser
                                    .parseString(dbExp.getEvidenciaTecnicaJson()).getAsJsonObject();
                            if (jsonEvidencia.has("ancho")) {
                                int dbAncho = jsonEvidencia.get("ancho").getAsInt();
                                int dbAlto = jsonEvidencia.get("alto").getAsInt();
                                String dbExt = jsonEvidencia.has("extensionReal")
                                        ? jsonEvidencia.get("extensionReal").getAsString()
                                        : "";

                                int currentAncho = imagen.getMetadatos().getAncho();
                                int currentAlto = imagen.getMetadatos().getAlto();
                                String currentExt = imagen.getMetadatos().getExtensionReal();

                                if (dbAncho != currentAncho || dbAlto != currentAlto
                                        || !dbExt.equalsIgnoreCase(currentExt)) {
                                    throw new RuntimeException(
                                            "Forense: No certificamos variantes por políticas de integridad y conservación de pruebas.");
                                }
                            }
                        } catch (Exception e) {
                            if (e instanceof RuntimeException && e.getMessage().startsWith("Forense:")) {
                                throw e;
                            }
                            System.err.println("No se pudo parsear el JSON de evidencia para validar duplicado visual: "
                                    + e.getMessage());
                        }

                        expAnterior = dbExp;
                        break;
                    }
                }
            }

            // Si la IMAGEN es un duplicado exacto o visual idéntico, saltamos el PSD por completo
            if (esDuplicadoBloqueante(expAnterior)) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.getHashImagenFinal());
                return resultado;
            }

            // 2. Procesar PSD utilizando caché
            String sha512PSD = hashPort.calcularSHA512(psdFile);
            contexto.setSha512PSD(sha512PSD);

            ArchivoPSD psd;
            String psdPHashStr;

            ProcesoCacheManager.PsdCacheEntry entry = cacheManager.obtenerPsd(sha512PSD);
            if (entry != null) {
                System.out.println("[Orchestrator] Usando análisis PSD desde caché para hash: " + sha512PSD);
                psd = entry.psd;
                psdPHashStr = entry.pHash;
            } else {
                @SuppressWarnings("unchecked")
                ArchivoProcessorPort<ArchivoPSD> processor = (ArchivoProcessorPort<ArchivoPSD>) factory.getProcessor(psdFile);
                psd = processor.procesar(psdFile);
                BufferedImage imgPSD = ImageLoader.loadWithSubsampling(psdFile);
                psdPHashStr = calcPHash.generarHash(imgPSD);
                if (imgPSD != null) {
                    imgPSD.flush();
                }
                cacheManager.guardarPsd(sha512PSD, psd, psdPHashStr);
            }

            contexto.setArchivoPSD(psd);

            VeredictoFinal veredictoPSD = validadorPSD.validar(psd);
            if (veredictoPSD.isEsRechazado()) {
                throw new RuntimeException("Rechazo PSD: " + veredictoPSD.getRazonRechazo());
            }
            contexto.setVeredictoPSD(veredictoPSD);

            contexto.setCapasPSD(psd.getCapas() != null ? psd.getCapas().size() : 0);
            contexto.setMetadatosDetectados(psd.getMetadatos() != null);

            // 3. Comparar pHash (Similitud Visual)
            double similitud = calcPHash.compararSimilitud(psdPHashStr, pHashStr);
            if (similitud < 90.0) {
                throw new RuntimeException("Rechazo Imagen: La similitud visual pHash no es suficiente ("
                        + String.format("%.2f", similitud) + "%). El PSD y la imagen no coinciden visualmente.");
            }

            // Verificar Duplicados por el hash del PSD
            if (expAnterior == null) {
                expAnterior = expedienteRepository.buscarPorHashPsd(contexto.getSha512PSD()).orElse(null);
            }

            if (esDuplicadoBloqueante(expAnterior)) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("estado", "REQUIERE_CEDULA");
                resultado.put("hash_duplicado", expAnterior.getHashImagenFinal());
                return resultado;
            }

            // Guardar borrador en base de datos (Arquitectura Stateless MicroProfile)
            com.google.gson.JsonObject jsonEv = new com.google.gson.JsonObject();
            if (imagen.getMetadatos() != null) {
                jsonEv.addProperty("ancho", imagen.getMetadatos().getAncho());
                jsonEv.addProperty("alto", imagen.getMetadatos().getAlto());
                jsonEv.addProperty("extensionReal", imagen.getMetadatos().getExtensionReal());
                jsonEv.addProperty("dpi", imagen.getMetadatos().getDpiCalculado());
            }
            jsonEv.addProperty("capasPSD", psd.getCapas() != null ? psd.getCapas().size() : 0);
            jsonEv.addProperty("metadatosDetectados", psd.getMetadatos() != null);

            expedienteRepository.crearBorradorAnalisis(expedienteId, usuarioId, sha512PSD, sha512Imagen,
                    pHashStr, jsonEv.toString(), contexto.getImagenRaw());

            Map<String, String> resultado = new HashMap<>();
            resultado.put("expediente_id", expedienteId);
            resultado.put("estado", "ANALIZADO");

            if (psd.getMetadatos() != null && psd.getMetadatos().getSoftware() != null) {
                resultado.put("software_detectado", psd.getMetadatos().getSoftware());
            } else if (imagen.getMetadatos() != null && imagen.getMetadatos().getSoftware() != null) {
                resultado.put("software_detectado", imagen.getMetadatos().getSoftware());
            }

            return resultado;
        } finally {
            try { Files.deleteIfExists(psdFile.toPath()); } catch (Exception ignored) {}
            try { Files.deleteIfExists(imgFile.toPath()); } catch (Exception ignored) {}
        }
    }
}
