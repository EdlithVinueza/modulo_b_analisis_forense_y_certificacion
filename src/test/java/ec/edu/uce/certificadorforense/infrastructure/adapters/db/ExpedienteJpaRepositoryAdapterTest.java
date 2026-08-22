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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración del refactor de persistencia (2026-08-22): confirma que
 * ExpedienteJpaRepositoryAdapter, ahora la única vía de persistencia para el
 * orquestador, escribe y lee correctamente contra la base de datos real —
 * sin pasar por el pipeline forense completo (que requiere PSD/imagen reales
 * y ya tiene sus propios datos de prueba certificados, lo que bloquea un
 * registro nuevo por la regla anti-duplicados).
 */
@QuarkusTest
public class ExpedienteJpaRepositoryAdapterTest {

    @Inject
    ExpedienteRepositoryPort repo;

    @Test
    @TestTransaction
    void flujoCompletoDeRegistroFirmaYCertificacionPersisteCorrectamente() {
        UsuarioEntity usuario = UsuarioEntity.find("cedula", "9900000003").firstResult();
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
                "127.0.0.1", hashPsd, hashImagen, phash, "{\"ancho\":100,\"alto\":100,\"extensionReal\":\"png\"}");

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
        repo.actualizarObraYDeclaraciones(idExpediente, usuario.id, obraActualizada, CategoriaObra.ILUSTRACION, decl, "127.0.0.1");

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
        UsuarioEntity usuario = UsuarioEntity.find("cedula", "9900000003").firstResult();
        assertNotNull(usuario);

        String idExpediente = UUID.randomUUID().toString();
        Obra obra = Obra.builder().titulo("Borrador").descripcion("d").software("s").hardware("h")
                .categoria(CategoriaObra.ILUSTRACION).fechaCreacion(LocalDate.now()).build();
        Declaraciones decl = Declaraciones.builder().titularDerechos(true).aceptaTerminos(true).build();

        repo.registrarNuevaObraYExpediente(idExpediente, usuario.id, obra, CategoriaObra.ILUSTRACION, decl,
                "127.0.0.1", "hash-psd-borrador-" + idExpediente, "hash-img-borrador-" + idExpediente,
                "phash-borrador-" + idExpediente, "{}");
        // En producción, eliminarBorrador siempre actúa sobre una fila ya persistida en una
        // transacción/request anterior — nunca sobre algo recién insertado en la misma transacción.
        // Flush explícito aquí para replicar esa condición real dentro del test.
        UsuarioEntity.getEntityManager().flush();

        ExpedienteResumen resumen = repo.buscarResumenPorId(idExpediente).orElseThrow();
        assertEquals("ESPERANDO_FIRMA", resumen.getEstadoActual());

        repo.eliminarBorrador(resumen.getObraId());

        assertTrue(repo.buscarResumenPorId(idExpediente).isEmpty(), "El expediente debe desaparecer tras eliminarBorrador");
    }
}
