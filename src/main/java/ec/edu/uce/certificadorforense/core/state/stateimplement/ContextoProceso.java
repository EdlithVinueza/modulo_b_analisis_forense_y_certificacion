package ec.edu.uce.certificadorforense.core.state.stateimplement;

import ec.edu.uce.certificadorforense.core.state.stateinterface.EstadoProceso;
import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.certificado.Certificado;
import ec.edu.uce.certificadorforense.core.model.expediente.Expediente;
import ec.edu.uce.certificadorforense.core.model.firma.FirmaAutor;
import ec.edu.uce.certificadorforense.core.model.obra.Declaraciones;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import ec.edu.uce.certificadorforense.core.model.modelimplement.psd.ArchivoPSD;
import ec.edu.uce.certificadorforense.core.model.modelimplement.imagen.ArchivoImagen;
import ec.edu.uce.certificadorforense.core.model.validacion.VeredictoFinal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContextoProceso {
    private EstadoProceso estadoActual;
    private ArchivoPSD archivoPSD;
    private ArchivoImagen archivoImagen;
    private byte[] imagenRaw;
    private VeredictoFinal veredictoPSD;
    private VeredictoFinal veredictoImagen;
    private String sha512PSD;
    private String sha512Imagen;
    private String pHash;
    private int capasPSD;
    private boolean metadatosDetectados;
    private Autor autor;
    private Obra obra;
    private Declaraciones declaraciones;
    private Expediente expediente;
    private String expedienteJson;
    private FirmaAutor firmaAutor;
    private Certificado certificado;
    private byte[] pdfCertificado;
    private byte[] imagenCertificada;
    private byte[] zipGenerado;

    public ContextoProceso(EstadoProceso estadoInicial) {
        this.estadoActual = estadoInicial;
    }
}
