package uteq.edu.ec.crecuteq.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.crecuteq.dto.SpringerApcRequestDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// INICIO - Importación PDF Springer
class SpringerPdfImportServiceTests {

    private final SpringerPdfImportService service =
            new SpringerPdfImportService(
                    mock(SpringerRevistaService.class),
                    mock(AuditoriaService.class)
            );

    @Test
    void interpretaFilasOficialesSinPosicionesFijas() {
        String texto = """
                Springer Nature Fully OA Journals List 2026
                APCs correct as of April 2026
                Journal title Imprint eISSN 2026 EUR 2026 USD 2026 GBP Website
                3D Printing in Medicine BioMed Central 2365-6271 1,790 2,090 1,390
                Advanced Biotechnology Springer 2948-2801 see website see website see website https://link.springer.com/journal/44307
                """;

        SpringerPdfImportService.ParseoPdf resultado =
                service.extraerRegistrosPagina(texto, 2026);

        assertThat(resultado.rechazados()).isZero();
        assertThat(resultado.registros()).hasSize(2);

        SpringerApcRequestDTO primero = resultado.registros().get(0);
        assertThat(primero.getTitulo()).isEqualTo("3D Printing in Medicine");
        assertThat(primero.getImprint()).isEqualTo("BioMed Central");
        assertThat(primero.getEissn()).isEqualTo("2365-6271");
        assertThat(primero.getApcEur()).isEqualTo("1,790");
        assertThat(primero.getApcUsd()).isEqualTo("2,090");
        assertThat(primero.getApcGbp()).isEqualTo("1,390");
        assertThat(primero.getAnioVigencia()).isEqualTo(2026);

        SpringerApcRequestDTO segundo = resultado.registros().get(1);
        assertThat(segundo.getApcEur()).isEqualTo("see website");
        assertThat(segundo.getUrlOficial())
                .isEqualTo("https://link.springer.com/journal/44307");
    }

    @Test
    void procesaPaginaSinEncabezadoRepetidoYRechazaPrecioCompuesto() {
        String texto = """
                AI Perspectives & Advances Springer 2948-2143 1,590 1,890 1,240
                Advances in Therapy Adis 1865-8652 OA APC 3990 + (6500 Mandatory RSF) OA APC 4990 + (8385 Mandatory RSF) OA APC 3090 + (5600 Mandatory RSF)
                """;

        SpringerPdfImportService.ParseoPdf resultado =
                service.extraerRegistrosPagina(texto, 2026);

        assertThat(resultado.registros()).hasSize(1);
        assertThat(resultado.registros().get(0).getTitulo())
                .isEqualTo("AI Perspectives & Advances");
        assertThat(resultado.rechazados()).isEqualTo(1);
    }

    @Test
    void conservaValoresEspecialesSinConvertirlosANumero() {
        String texto = """
                Journal title Imprint eISSN EUR USD GBP Website
                Revista Especial Springer 2365-6271 Free N/A not applicable
                """;

        SpringerApcRequestDTO registro =
                service.extraerRegistrosPagina(texto, 2026)
                        .registros().get(0);

        assertThat(registro.getApcEur()).isEqualTo("Free");
        assertThat(registro.getApcUsd()).isEqualTo("N/A");
        assertThat(registro.getApcGbp()).isEqualTo("not applicable");
    }

    @Test
    void extraeEissnCuandoOpenPdfLoConcatenaConElPrecio() {
        String texto = """
                Journal of Statistical PhysicsSpringer1572-96132,590 3,190 2,290
                """;

        SpringerPdfImportService.ParseoPdf resultado =
                service.extraerRegistrosPagina(texto, 2026);

        assertThat(resultado.rechazados()).isZero();
        assertThat(resultado.registros()).hasSize(1);
        SpringerApcRequestDTO registro = resultado.registros().get(0);
        assertThat(registro.getTitulo()).isEqualTo("Journal of Statistical Physics");
        assertThat(registro.getEissn()).isEqualTo("1572-9613");
        assertThat(registro.getApcEur()).isEqualTo("2,590");
        assertThat(registro.getApcUsd()).isEqualTo("3,190");
        assertThat(registro.getApcGbp()).isEqualTo("2,290");
    }

    @Test
    void registraConteosYAuditoriaEnImportacionManual() throws Exception {
        SpringerRevistaService revistaService =
                mock(SpringerRevistaService.class);
        AuditoriaService auditoriaService = mock(AuditoriaService.class);
        SpringerPdfImportService servicio = spy(
                new SpringerPdfImportService(revistaService, auditoriaService)
        );
        SpringerApcRequestDTO primero = new SpringerApcRequestDTO();
        SpringerApcRequestDTO segundo = new SpringerApcRequestDTO();
        doReturn(new SpringerPdfImportService.ParseoPdf(
                List.of(primero, segundo),
                1
        )).when(servicio).extraerRegistros(any());
        when(revistaService.actualizarApc(primero)).thenReturn(true);
        when(revistaService.actualizarApc(segundo)).thenReturn(false);

        var respuesta = servicio.importar(new MockMultipartFile(
                "archivo",
                "apc.pdf",
                "application/pdf",
                "%PDF-contenido-prueba".getBytes()
        ));

        assertThat(respuesta.getRegistrosLeidos()).isEqualTo(3);
        assertThat(respuesta.getApcActualizados()).isEqualTo(1);
        assertThat(respuesta.getRegistrosRechazados()).isEqualTo(1);
        assertThat(respuesta.getRegistrosSinCoincidencia()).isEqualTo(1);
        verifyNoInteractions(auditoriaService);
    }
}
// FIN - Importación PDF Springer
