package ec.edu.uce.certificadorforense.application.service;

import ec.edu.uce.certificadorforense.core.model.expediente.UsuarioDatos;
import ec.edu.uce.certificadorforense.core.ports.in.EmitirCertificadoUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.FirmarExpedienteUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.IniciarAnalisisUseCase;
import ec.edu.uce.certificadorforense.core.ports.in.RegistrarDatosObraUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.util.Map;

/**
 * Fachada de aplicación que preserva compatibilidad con clientes existentes
 * delegando directamente a los Casos de Uso (Arquitectura Hexagonal).
 */
@ApplicationScoped
public class CertificacionOrchestrator {

    @Inject
    IniciarAnalisisUseCase iniciarAnalisisUseCase;

    @Inject
    RegistrarDatosObraUseCase registrarDatosObraUseCase;

    @Inject
    FirmarExpedienteUseCase firmarExpedienteUseCase;

    @Inject
    EmitirCertificadoUseCase emitirCertificadoUseCase;

    public Map<String, String> iniciarAnalisisFase1(File psdFile, File imgFile, String extension) throws Exception {
        return iniciarAnalisisUseCase.ejecutar(psdFile, imgFile, extension);
    }

    public void registrarDatosFase2(String idExpediente, UsuarioDatos usuarioDb, Map<String, Object> body) {
        registrarDatosObraUseCase.ejecutar(idExpediente, usuarioDb, body);
    }

    public String firmarFase3(String idExpediente, String password) throws Exception {
        return firmarExpedienteUseCase.ejecutar(idExpediente, password);
    }

    public byte[] emitirCertificadoFase4(String idExpediente) throws Exception {
        return emitirCertificadoUseCase.emitir(idExpediente);
    }

    public boolean esPropietario(String idExpediente, String cedulaSolicitante) {
        return emitirCertificadoUseCase.esPropietario(idExpediente, cedulaSolicitante);
    }

    public byte[] obtenerZipYLimpiar(String idExpediente) {
        return emitirCertificadoUseCase.obtenerZipYLimpiar(idExpediente);
    }
}
