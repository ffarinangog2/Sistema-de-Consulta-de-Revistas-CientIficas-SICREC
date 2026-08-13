package uteq.edu.ec.crecuteq.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.dto.JournalInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// INICIO - Registro extensible de estrategias de proveedores
@Service
public class JournalProviderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalProviderRegistry.class);
    private final Map<String, JournalProvider> providers;

    public JournalProviderRegistry(List<JournalProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.nombre().toLowerCase(Locale.ROOT),
                Function.identity()
        ));
    }

    public Optional<JournalInfo> buscar(
            String proveedor, String productId, String issn, String eissn
    ) {
        JournalProvider strategy = providers.get(normalizar(proveedor));
        return strategy == null
                ? Optional.empty()
                : buscarSeguro(strategy, productId, issn, eissn);
    }

    public Map<String, JournalInfo> buscarTodos(
            String productId, String issn, String eissn
    ) {
        Map<String, JournalInfo> resultados = new LinkedHashMap<>();
        providers.forEach((nombre, strategy) ->
                buscarSeguro(strategy, productId, issn, eissn)
                        .ifPresent(info -> resultados.put(nombre, info)));
        return resultados;
    }

    private Optional<JournalInfo> buscarSeguro(
            JournalProvider strategy, String productId, String issn, String eissn
    ) {
        try {
            return strategy.buscar(productId, issn, eissn);
        } catch (RuntimeException exception) {
            LOGGER.warn("No fue posible consultar {}: {}",
                    strategy.nombre(), exception.getMessage());
            return Optional.empty();
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
// FIN - Registro extensible de estrategias de proveedores
