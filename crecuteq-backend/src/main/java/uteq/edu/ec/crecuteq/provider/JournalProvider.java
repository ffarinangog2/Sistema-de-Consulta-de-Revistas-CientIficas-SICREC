package uteq.edu.ec.crecuteq.provider;

import uteq.edu.ec.crecuteq.dto.JournalInfo;

import java.util.Optional;

// INICIO - Patrón Strategy para proveedores
public interface JournalProvider {
    String nombre();
    Optional<JournalInfo> buscar(String productId, String issn, String eissn);
}
// FIN - Patrón Strategy para proveedores
