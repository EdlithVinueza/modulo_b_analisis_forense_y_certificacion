package ec.edu.uce.certificadorforense.infrastructure.adapters.firma;

import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.ports.out.FirmadorExpedientePort;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Enumeration;

/**
 * Adaptador de infraestructura: firma del expediente JSON con el P12 del autor.
 * <p>
 * Usa exclusivamente {@code java.security} (PKCS12 + SHA512withRSA).
 * Sin dependencias externas de criptografía.
 * </p>
 */
public class FirmadorP12Adapter implements FirmadorExpedientePort {

    @Override
    public void validar(File archivoPkcs12, String contrasena) {
        try {
            // 1 & 3. El archivo es un PKCS#12 válido y no está corrupto (ks.load verifica esto internamente)
            KeyStore ks = cargarKeystore(archivoPkcs12, contrasena);
            String alias = obtenerAlias(ks);
            
            // 2. Contiene un certificado X.509
            java.security.cert.Certificate rawCert = ks.getCertificate(alias);
            if (rawCert == null) {
                throw new FirmaException("No se encontró ningún certificado en el archivo P12.");
            }
            if (!(rawCert instanceof X509Certificate)) {
                throw new FirmaException("El certificado no es de tipo X.509 válido.");
            }
            X509Certificate cert = (X509Certificate) rawCert;

            // 4. La firma de la obra será posible: verificamos que exista la clave privada correspondiente
            if (!ks.isKeyEntry(alias)) {
                throw new FirmaException("El certificado no tiene una clave privada asociada para poder firmar.");
            }

            // 5. La fecha actual está dentro de la vigencia
            cert.checkValidity();

            System.out.println("[FirmadorP12Adapter] Validación de certificado exitosa: " + cert.getSubjectX500Principal().getName());

        } catch (FirmaException e) {
            throw e;
        } catch (java.security.cert.CertificateExpiredException e) {
            throw new FirmaException("El certificado está caducado o expiró.", e);
        } catch (java.security.cert.CertificateNotYetValidException e) {
            throw new FirmaException("El certificado aún no entra en periodo de validez.", e);
        } catch (Exception e) {
            throw new FirmaException("Validación del P12 fallida: " + e.getMessage(), e);
        }
    }

    @Override
    public FirmaAutor firmar(String expedienteJson, File archivoPkcs12, String contrasena) {
        try {
            KeyStore ks = cargarKeystore(archivoPkcs12, contrasena);
            String alias = obtenerAlias(ks);

            PrivateKey clavePrivada = (PrivateKey) ks.getKey(alias, contrasena.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            // Verificar vigencia antes de firmar
            cert.checkValidity();

            // Calcular hash del expediente
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(expedienteJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashHex = new StringBuilder();
            for (byte b : hashBytes) hashHex.append(String.format("%02x", b));

            // Firmar con SHA512withRSA
            Signature firma = Signature.getInstance("SHA512withRSA");
            firma.initSign(clavePrivada);
            firma.update(expedienteJson.getBytes(StandardCharsets.UTF_8));
            byte[] firmaBytes = firma.sign();
            String firmaBase64 = Base64.getEncoder().encodeToString(firmaBytes);

            System.out.println("[FirmadorP12Adapter] Expediente firmado exitosamente. Alias: " + alias);

            return FirmaAutor.builder()
                    .hashExpediente(hashHex.toString())
                    .firmaBase64(firmaBase64)
                    .algoritmo("SHA512withRSA")
                    .fechaFirma(Instant.now())
                    .aliasKeystore(alias)
                    .build();

        } catch (FirmaException e) {
            throw e;
        } catch (Exception e) {
            throw new FirmaException("Error al firmar el expediente: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private KeyStore cargarKeystore(File archivo, String contrasena) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(archivo)) {
                ks.load(fis, contrasena.toCharArray());
            }
            return ks;
        } catch (Exception e) {
            throw new FirmaException(
                "No se pudo cargar el keystore P12 '" + archivo.getName()
                + "'. Verifique la contraseña y el archivo.", e
            );
        }
    }

    private String obtenerAlias(KeyStore ks) {
        try {
            Enumeration<String> aliases = ks.aliases();
            if (!aliases.hasMoreElements()) {
                throw new FirmaException("El keystore P12 no contiene ningún alias.");
            }
            return aliases.nextElement();
        } catch (KeyStoreException e) {
            throw new FirmaException("Error accediendo a los alias del keystore.", e);
        }
    }
}
