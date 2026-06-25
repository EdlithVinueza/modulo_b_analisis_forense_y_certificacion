package ec.edu.uce.certificadorforense.core.model.obra;

/**
 * Categorías de obra digital reconocidas por el sistema Verisart.
 */
public enum CategoriaObra {

    ILUSTRACION("Ilustración"),
    DISENO_GRAFICO("Diseño gráfico"),
    ARTE_CONCEPTUAL("Arte conceptual"),
    FOTOMANIPULACION("Fotomanipulación"),
    OTRO("Otro");

    private final String etiqueta;

    CategoriaObra(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
