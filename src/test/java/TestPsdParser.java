import ec.edu.uce.certificadorforense.infrastructure.adapters.processors.ExtractorCapasPSD;
import ec.edu.uce.certificadorforense.core.model.psd.EstructuraCapaPSD;
import java.util.List;
public class TestPsdParser {
    public static void main(String[] args) {
        String[] files = {
            "../Archivos de Prueba/psd/girasol-captura-navegador-edge.psd",
            "../Archivos de Prueba/psd/girasol-captura-windows.psd",
            "../Archivos de Prueba/psd/girasoles-original.psd",
            "../Archivos de Prueba/psd/girasol-original-una-capa.psd",
            "../Archivos de Prueba/psd/hydrangeas.psd"
        };
        for (String f : files) {
            try {
                List<EstructuraCapaPSD> capas = ExtractorCapasPSD.extraer(f);
                System.out.println("File: " + f + " -> capas: " + capas.size());
            } catch (Exception e) {
                System.out.println("File: " + f + " -> ERROR: " + e.getMessage());
            }
        }
    }
}
