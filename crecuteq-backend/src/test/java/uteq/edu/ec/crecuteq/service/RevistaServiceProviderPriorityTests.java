package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.crecuteq.dto.JournalInfo;
import uteq.edu.ec.crecuteq.dto.RevistaDTO;
import uteq.edu.ec.crecuteq.provider.JournalProviderRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// INICIO - Pruebas de prioridad entre Springer y DOAJ
class RevistaServiceProviderPriorityTests {

    @Test
    void utilizaSpringerCuandoLaRevistaTambienExisteEnDoaj() {
        JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
        JournalInfo springer = journal(
                "springer", "Springer Nature", "Título Springer");
        JournalInfo doaj = journal("doaj", "Otra editorial", "Título DOAJ");
        when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                .thenReturn(resultados(springer, doaj));

        RevistaDTO dto = complementar(registry);

        assertThat(dto.getSpringer()).isNotNull();
        assertThat(dto.getSpringer().getTitulo()).isEqualTo("Título Springer");
        assertThat(dto.getDoaj()).isNotNull();
        assertThat(dto.getProveedores()).containsOnlyKeys("springer", "doaj");
    }

    @Test
    void utilizaDoajCuandoLaCoincidenciaNoPerteneceAEditorialSpringer() {
        JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
        JournalInfo springer = journal(
                "springer", "Editorial independiente", "Título alternativo");
        JournalInfo doaj = journal("doaj", "Editorial UTEQ", "Título DOAJ");
        when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                .thenReturn(resultados(springer, doaj));

        RevistaDTO dto = complementar(registry);

        assertThat(dto.getSpringer()).isNotNull();
        assertThat(dto.getDoaj()).isSameAs(doaj);
        assertThat(dto.getProveedores()).containsOnlyKeys("springer", "doaj");
    }

    @Test
    void reconoceSpringerOpenYBmcComoEditorialesSpringer() {
        for (String editorial : new String[]{"SpringerOpen", "BMC Medicine"}) {
            JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
            JournalInfo springer = journal("springer", editorial, editorial);
            when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                    .thenReturn(resultados(
                            springer, journal("doaj", "DOAJ", "DOAJ")));

            assertThat(complementar(registry).getSpringer()).isNotNull();
        }
    }

    @Test
    void priorizaFuenteOficialDeEditorialScopus() {
        JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
        JournalInfo ieee = journalConApc("ieee", "IEEE", "IEEE Journal", "2160");
        JournalInfo doaj = journalConApc("doaj", "IEEE", "IEEE Journal", "1800");
        when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                .thenReturn(Map.of("doaj", doaj, "ieee", ieee));

        RevistaDTO dto = complementar(registry, "Institute of Electrical and Electronics Engineers (IEEE)");

        assertThat(dto.getProveedores().get("ieee").getApc()).isEqualTo("2160");
        assertThat(dto.getProveedores().get("ieee").getDatosAdicionales())
                .containsEntry("prioridadApc", "editorial-oficial");
        assertThat(dto.getProveedores().get("doaj").getApc()).isNull();
    }

    @Test
    void usaRespaldoExactoCuandoFuenteOficialNoTieneApc() {
        JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
        JournalInfo ieeeSinApc = journal("ieee", "IEEE", "IEEE Journal");
        JournalInfo doaj = journalConApc("doaj", "IEEE", "IEEE Journal", "1800");
        when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                .thenReturn(Map.of("ieee", ieeeSinApc, "doaj", doaj));

        RevistaDTO dto = complementar(registry, "IEEE");

        assertThat(dto.getProveedores().get("doaj").getApc()).isEqualTo("1800");
        assertThat(dto.getProveedores().get("doaj").getDatosAdicionales())
                .containsEntry("prioridadApc", "respaldo");
    }

    @Test
    void rechazaApcDeOtraRevistaAunqueSeaLaEditorialOficial() {
        JournalProviderRegistry registry = mock(JournalProviderRegistry.class);
        JournalInfo ieeeIncorrecto = new JournalInfo(
                "ieee", "Título parecido", "9999-9999", null, "IEEE",
                null, null, null, null, "2160", "USD", null, Map.of());
        JournalInfo doaj = journalConApc("doaj", "IEEE", "Revista correcta", "1800");
        when(registry.buscarTodos(null, "1234-5678", "2049-3630"))
                .thenReturn(Map.of("ieee", ieeeIncorrecto, "doaj", doaj));

        RevistaDTO dto = complementar(registry, "IEEE");

        assertThat(dto.getProveedores()).doesNotContainKey("ieee");
        assertThat(dto.getProveedores().get("doaj").getApc()).isEqualTo("1800");
    }

    private RevistaDTO complementar(JournalProviderRegistry registry) {
        return complementar(registry, null);
    }

    private RevistaDTO complementar(JournalProviderRegistry registry, String editorial) {
        RevistaService service = new RevistaService(
                mock(), mock(), mock(), mock(), registry);
        RevistaDTO dto = new RevistaDTO();
        ReflectionTestUtils.invokeMethod(
                service, "complementarProveedores",
                dto, null, "1234-5678", "2049-3630", editorial);
        return dto;
    }

    private Map<String, JournalInfo> resultados(
            JournalInfo springer, JournalInfo doaj
    ) {
        Map<String, JournalInfo> resultado = new LinkedHashMap<>();
        resultado.put("springer", springer);
        resultado.put("doaj", doaj);
        return resultado;
    }

    private JournalInfo journal(
            String proveedor, String editorial, String titulo
    ) {
        return new JournalInfo(
                proveedor, titulo, "1234-5678", "2049-3630",
                editorial, null, null, null, null, null, null, null,
                Map.of());
    }

    private JournalInfo journalConApc(
            String proveedor, String editorial, String titulo, String apc
    ) {
        return new JournalInfo(
                proveedor, titulo, "1234-5678", "2049-3630",
                editorial, null, null, null, null, apc, "USD", null,
                Map.of("apcUsd", apc));
    }
}
// FIN - Pruebas de prioridad entre Springer y DOAJ
