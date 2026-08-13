package uteq.edu.ec.crecuteq.provider;

import org.springframework.stereotype.Component;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.service.SpringerImportService;
import uteq.edu.ec.crecuteq.service.SpringerRevistaService;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Optional;

// INICIO - Estrategia Springer
@Component
public class SpringerProvider implements JournalProvider {
    private final SpringerRevistaService service;
    private final SpringerImportService importService;

    public SpringerProvider(
            SpringerRevistaService service,
            SpringerImportService importService
    ) {
        this.service = service;
        this.importService = importService;
    }

    @Override
    public String nombre() {
        return "springer";
    }

    @Override
    public Optional<JournalInfo> buscar(
            String productId, String issn, String eissn
    ) {
        return service.buscarParaBusqueda(productId, issn, eissn).map(info -> {
            var adicionales = new LinkedHashMap<String, String>();
            agregar(adicionales, "periodicidadEstimada", info.getPeriodicidadEstimada());
            agregar(adicionales, "apcEur", info.getApcEur());
            agregar(adicionales, "apcUsd", info.getApcUsd());
            agregar(adicionales, "apcGbp", info.getApcGbp());
            return new JournalInfo(
                    nombre(), null, info.getIssn(), info.getEissn(),
                    null, null, null, null, null,
                    primerNoVacio(info.getApcUsd(), info.getApcEur(), info.getApcGbp()),
                    obtenerMonedaApc(
                            info.getApcUsd(), info.getApcEur(), info.getApcGbp()),
                    null, adicionales
            );
        });
    }

    public CatalogImportResponseDTO importar(MultipartFile archivo) {
        return importService.importar(archivo);
    }

    private void agregar(LinkedHashMap<String, String> datos, String clave, String valor) {
        if (valor != null) datos.put(clave, valor);
    }

    private String primerNoVacio(String... valores) {
        for (String valor : valores) if (valor != null) return valor;
        return null;
    }

    // INICIO - Moneda APC neutral para JournalInfo
    private String obtenerMonedaApc(String usd, String eur, String gbp) {
        if (usd != null) return "USD";
        if (eur != null) return "EUR";
        return gbp != null ? "GBP" : null;
    }
    // FIN - Moneda APC neutral para JournalInfo
}
// FIN - Estrategia Springer
