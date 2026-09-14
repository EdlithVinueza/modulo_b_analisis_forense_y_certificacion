package ec.edu.uce.certificadorforense.infrastructure.adapters.db;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.ExpedienteResumen;
import ec.edu.uce.certificadorforense.core.model.expediente.RecuperacionDatos;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.CategoriaObra;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.ports.out.ExpedienteRepositoryPort;
import ec.edu.uce.certificadorforense.infrastructure.adapters.db.entity.UsuarioEntity;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración de persistencia JPA.
 * Garantiza que si el usuario de prueba no existe previamente en la BD local,
 * se provisiona automáticamente dentro de la transacción de prueba hermética.
 */
@QuarkusTest
public class ExpedienteJpaRepositoryAdapterTest {

    @Inject
    ExpedienteRepositoryPort repo;

    private UsuarioEntity obtenerOCrearUsuarioPrueba() {
        UsuarioEntity usuario = UsuarioEntity.find("cedula", "9900000003").firstResult();
        if (usuario == null) {
            usuario = new UsuarioEntity();
            usuario.cedula = "9900000003";
            usuario.cedulaHash = "test-hash-cedula-9900000003";
            usuario.nombres = "Artista Test";
            usuario.apellidos = "Prueba";
            usuario.correo = "artista.test@example.com";
            usuario.nombreArtistico = "ArtTest";
            usuario.passwordHash = "test-hash";
            usuario.firmaP12 = "ZmlybWEtZGUtcHJ1ZWJh";
            usuario.aceptaTerminosPlataforma = true;
            usuario.fechaRegistro = LocalDateTime.now();
            usuario.activo = true;
            usuario.persistAndFlush();
        }
        return usuario;
    }

    @Test
    @TestTransaction
    void flujoCompletoDeRegistroFirmaYCertificacionPersisteCorrectamente() {
        UsuarioEntity usuario = obtenerOCrearUsuarioPrueba();
        assertNotNull(usuario, "Debe existir el usuario de prueba (cedula 9900000003) en la BD para este test");

        String idExpediente = UUID.randomUUID().toString();
        String hashPsd = "test-hash-psd-" + idExpediente;
        String hashImagen = "test-hash-imagen-" + idExpediente;
        String phash = "test-phash-" + idExpediente;

        Obra obra = Obra.builder()
                .titulo("Obra de prueba (integración)")
                .descripcion("Generada por test automático de persistencia")
                .software("Test")
                .hardware("Test")
                .categoria(CategoriaObra.ILUSTRACION)
                .fechaCreacion(LocalDate.now())
                .build();

        Declaraciones decl = Declaraciones.builder()
                .titularDerechos(true)
                .aceptaTerminos(true)
                .build();

        // 1. Registro (Fase 2, camino nuevo)
        repo.registrarNuevaObraYExpediente(idExpediente, usuario.id, obra, CategoriaObra.ILUSTRACION, decl,
                hashPsd, hashImagen, phash, "{\"ancho\":100,\"alto\":100,\"extensionReal\":\"png\"}");

        Optional<ExpedienteResumen> resumen = repo.buscarResumenPorId(idExpediente);
        assertTrue(resumen.isPresent(), "El expediente debe existir tras registrarNuevaObraYExpediente");
        assertEquals("ESPERANDO_FIRMA", resumen.get().getEstadoActual());
        assertEquals(hashPsd, resumen.get().getHashPsdOriginal());
        assertEquals(usuario.id, resumen.get().getUsuarioId());

        // También debe encontrarse por hash (chequeo de duplicados)
        assertTrue(repo.buscarPorHashPsd(hashPsd).isPresent(), "buscarPorHashPsd debe encontrar el expediente recién creado");
        assertTrue(repo.buscarPorHashImagen(hashImagen).isPresent(), "buscarPorHashImagen debe encontrar el expediente recién creado");

        // 2. Rectificación (Fase 2, camino de actualización)
        Obra obraActualizada = Obra.builder()
                .titulo("Obra de prueba (RECTIFICADA)")
                .descripcion("Descripción actualizada")
                .software("Test")
                .hardware("Test")
                .categoria(CategoriaObra.ILUSTRACION)
                .fechaCreacion(LocalDate.now())
                .build();
        repo.actualizarObraYDeclaraciones(idExpediente, usuario.id, obraActualizada, CategoriaObra.ILUSTRACION, decl);

        // 3. Firma (Fase 3)
        FirmaAutor firma = FirmaAutor.builder()
                .firmaBase64("ZmlybWEtZGUtcHJ1ZWJh")
                .hashExpediente("hash-expediente-firmado-test")
                .algoritmo("SHA512withRSA")
                .fechaFirma(Instant.now())
                .aliasKeystore("TEST")
                .build();
        repo.guardarFirma(idExpediente, firma);

        Optional<ExpedienteResumen> resumenFirmado = repo.buscarResumenPorId(idExpediente);
        assertEquals("CERTIFICADO", resumenFirmado.get().getEstadoActual(), "El estado debe pasar a CERTIFICADO tras guardarFirma");

        // 4. Certificado (Fase 4)
        Certificado certificado = Certificado.builder()
                .idCertificado("CERT-TEST-" + idExpediente.substring(0, 8))
                .idExpediente(idExpediente)
                .fechaEmision(Instant.now())
                .hashExpedienteFirmado("hash-final-test")
                .qrContenido("contenido-qr-test")
                .qrBase64("qr-base64-test")
                .build();
        repo.guardarCertificado(idExpediente, certificado, "{\"json\":\"del expediente firmado\"}\n---FIRMA---\nZmlybWE=");

        Optional<ExpedienteResumen> resumenFinal = repo.buscarResumenPorId(idExpediente);
        assertEquals("FINALIZADO", resumenFinal.get().getEstadoActual(), "El estado debe pasar a FINALIZADO tras guardarCertificado");

        // 5. Recuperación — debe poder reconstruirse el certificado desde el hash de imagen
        Optional<RecuperacionDatos> recuperado = repo.buscarParaRecuperacion(hashImagen);
        assertTrue(recuperado.isPresent(), "Debe poder recuperarse el certificado ya emitido por el hash de imagen");
        assertEquals("9900000003", recuperado.get().getUsuarioCedula());
        assertEquals(certificado.getIdCertificado(), recuperado.get().getCertificado().getIdCertificado());
    }

    @Test
    @TestTransaction
    void eliminarBorradorLimpiaElExpedienteHuerfanoCompleto() {
        UsuarioEntity usuario = obtenerOCrearUsuarioPrueba();
        assertNotNull(usuario);

        String idExpediente = UUID.randomUUID().toString();
        Obra obra = Obra.builder().titulo("Borrador").descripcion("d").software("s").hardware("h")
                .categoria(CategoriaObra.ILUSTRACION).fechaCreacion(LocalDate.now()).build();
        Declaraciones decl = Declaraciones.builder().titularDerechos(true).aceptaTerminos(true).build();

        repo.registrarNuevaObraYExpediente(idExpediente, usuario.id, obra, CategoriaObra.ILUSTRACION, decl,
                "hash-psd-borrador-" + idExpediente, "hash-img-borrador-" + idExpediente,
                "phash-borrador-" + idExpediente, "{}");

        UsuarioEntity.getEntityManager().flush();

        ExpedienteResumen resumen = repo.buscarResumenPorId(idExpediente).orElseThrow();
        assertEquals("ESPERANDO_FIRMA", resumen.getEstadoActual());

        repo.eliminarBorrador(resumen.getObraId());

        assertTrue(repo.buscarResumenPorId(idExpediente).isEmpty(), "El expediente debe desaparecer tras eliminarBorrador");
    }

    @Test
    @TestTransaction
    void flujoStatelessDesdeBorradorFase1HastaCertificadoFinalizadoPersisteCorrectamente() {
        UsuarioEntity usuario = obtenerOCrearUsuarioPrueba();
        assertNotNull(usuario);

        String idExpediente = UUID.randomUUID().toString();
        String hashPsd = "test-hash-psd-fase1-" + idExpediente;
        String hashImagen = "test-hash-img-fase1-" + idExpediente;
        String phash = "test-phash-fase1-" + idExpediente;
        byte[] bytesImagenMock = "imagen-binaria-de-prueba-fase1".getBytes();

        // 1. Fase 1: Creación del Borrador inicial (ANALIZADO)
        repo.crearBorradorAnalisis(idExpediente, usuario.id, hashPsd, hashImagen, phash,
                "{\"ancho\":500,\"alto\":500,\"extensionReal\":\"png\"}", bytesImagenMock);

        Optional<ExpedienteResumen> resumenFase1 = repo.buscarResumenPorId(idExpediente);
        assertTrue(resumenFase1.isPresent());
        assertEquals("ANALIZADO", resumenFase1.get().getEstadoActual());
        assertArrayEquals(bytesImagenMock, repo.obtenerImagenRaw(idExpediente));

        // 2. Fase 2: Actualización de datos de obra y declaraciones (ESPERANDO_FIRMA)
        Obra obra = Obra.builder()
                .titulo("Obra Stateless")
                .descripcion("Probando persistencia sin RAM")
                .categoria(CategoriaObra.ILUSTRACION)
                .software("Photoshop")
                .hardware("PC")
                .fechaCreacion(LocalDate.now())
                .build();
        Declaraciones decl = Declaraciones.builder().titularDerechos(true).aceptaTerminos(true).build();

        repo.actualizarObraYDeclaraciones(idExpediente, usuario.id, obra, CategoriaObra.ILUSTRACION, decl);

        Optional<ExpedienteResumen> resumenFase2 = repo.buscarResumenPorId(idExpediente);
        assertTrue(resumenFase2.isPresent());
        assertEquals("ESPERANDO_FIRMA", resumenFase2.get().getEstadoActual());
        assertEquals("Obra Stateless", resumenFase2.get().getObraTitulo());

        // 3. Fase 3: Firma del autor con JSON serializado (CERTIFICADO)
        String jsonExpedienteMock = "{\"idExpediente\":\"" + idExpediente + "\"}";
        FirmaAutor firma = FirmaAutor.builder()
                .firmaBase64("ZmlybWExMjM=")
                .hashExpediente("hash-obra-fase3")
                .algoritmo("SHA512withRSA")
                .fechaFirma(Instant.now())
                .aliasKeystore("AzureCloud")
                .expedienteJson(jsonExpedienteMock)
                .build();

        repo.guardarFirma(idExpediente, firma, jsonExpedienteMock);

        Optional<ExpedienteResumen> resumenFase3 = repo.buscarResumenPorId(idExpediente);
        assertTrue(resumenFase3.isPresent());
        assertEquals("CERTIFICADO", resumenFase3.get().getEstadoActual());

        Optional<FirmaAutor> firmaGuardada = repo.buscarFirmaPorExpedienteId(idExpediente);
        assertTrue(firmaGuardada.isPresent());
        assertEquals(jsonExpedienteMock, firmaGuardada.get().getExpedienteJson());
        assertEquals("ZmlybWExMjM=", firmaGuardada.get().getFirmaBase64());

        // 4. Fase 4: Emisión de Certificado y almacenamiento del ZIP (FINALIZADO)
        Certificado cert = Certificado.builder()
                .idCertificado("CERT-STATELESS-" + idExpediente.substring(0, 8))
                .idExpediente(idExpediente)
                .fechaEmision(Instant.now())
                .hashExpedienteFirmado("hash-cert-final")
                .qrContenido("qr")
                .qrBase64("qr64")
                .build();

        byte[] zipMock = "contenido-zip-simulado".getBytes();
        repo.guardarCertificado(idExpediente, cert, jsonExpedienteMock + "\n---FIRMA---\nZmlybWExMjM=", zipMock);

        Optional<ExpedienteResumen> resumenFase4 = repo.buscarResumenPorId(idExpediente);
        assertTrue(resumenFase4.isPresent());
        assertEquals("FINALIZADO", resumenFase4.get().getEstadoActual());

        byte[] zipRecuperado = repo.obtenerZipCertificado(idExpediente);
        assertNotNull(zipRecuperado);
        assertArrayEquals(zipMock, zipRecuperado);

        // Limpieza de imagen temporal
        repo.limpiarImagenRaw(idExpediente);
        assertNull(repo.obtenerImagenRaw(idExpediente), "La imagen temporal debe ser null tras limpiarImagenRaw");
    }
}
