package ec.edu.uce.certificadorforense.core.model.expediente;

import ec.edu.uce.certificadorforense.core.model.autor.Autor;
import ec.edu.uce.certificadorforense.core.model.obra.Obra;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Expediente {
    private final String idExpediente;
    private final String fechaRegistro;
    private final Autor autor;
    private final Obra obra;
    private final AnalisisResumen analisis;
    private final HashesEvidencia hashes;
}
