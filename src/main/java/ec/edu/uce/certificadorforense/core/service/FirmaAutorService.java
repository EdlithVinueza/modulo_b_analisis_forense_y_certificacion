package ec.edu.uce.certificadorforense.core.service;

import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.ports.out.FirmadorExpedientePort;

import java.io.File;

/**
 * Servicio de dominio: firma del expediente con el P12 del autor.
 * <p>
 * Orquesta la validación del P12 y la firma del JSON del expediente
 * delegando en {@link FirmadorExpedientePort} (infraestructura).
 * </p>
 */
public class FirmaAutorService {

    private final FirmadorExpedientePort firmador;

    public FirmaAutorService(FirmadorExpedientePort firmador) {
        this.firmador = firmador;
    }

    /**
     * Valida el archivo P12 del autor antes de firmar.
     * Verifica: existencia, contraseña correcta, no expirado, alias recuperable.
     *
     * @param archivoPkcs12 Archivo {@code .p12} del autor.
     * @param contrasena    Contraseña del keystore.
     * @throws FirmadorExpedientePort.FirmaException si alguna validación falla.
     */
    public void validar(File archivoPkcs12, String contrasena) {
        if (archivoPkcs12 == null || !archivoPkcs12.exists()) {
            throw new FirmadorExpedientePort.FirmaException(
                "El archivo P12 no existe: " + (archivoPkcs12 != null ? archivoPkcs12.getPath() : "null")
            );
        }
        firmador.validar(archivoPkcs12, contrasena);
    }

    /**
     * Firma el JSON del expediente con la clave privada del P12 del autor.
     *
     * @param expedienteJson JSON serializado del expediente.
     * @param archivoPkcs12  Archivo {@code .p12} del autor.
     * @param contrasena     Contraseña del keystore.
     * @return {@link FirmaAutor} con la firma en Base64 y metadatos.
     */
    public FirmaAutor firmar(String expedienteJson, File archivoPkcs12, String contrasena) {
        System.out.println("[FirmaAutorService] Firmando expediente con P12 del autor...");
        FirmaAutor firma = firmador.firmar(expedienteJson, archivoPkcs12, contrasena);
        System.out.println("[FirmaAutorService] Firma aplicada. Hash expediente: " + firma.getHashExpediente());
        return firma;
    }
}
