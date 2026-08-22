package ec.edu.uce.certificadorforense.core.model.expediente;

import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import lombok.Builder;
import lombok.Getter;

/**
 * Lo que RecuperacionCertificadoService necesita para reensamblar el ZIP de
 * un certificado ya emitido, sin tocar CertificadoEntity/ExpedienteForenseEntity
 * directamente.
 */
@Getter
@Builder
public class RecuperacionDatos {
    private String usuarioCedula;
    private Certificado certificado;
    private String expedienteFirmadoRaw;
}
