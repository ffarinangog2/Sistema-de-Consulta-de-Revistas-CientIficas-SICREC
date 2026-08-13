package uteq.edu.ec.crecuteq.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.crecuteq.dto.CatalogImportResponseDTO;
import uteq.edu.ec.crecuteq.dto.SpringerRevistaRequestDTO;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// INICIO - Importación Springer
@ExtendWith(MockitoExtension.class)
class SpringerImportServiceTests {

    @Mock
    private SpringerRevistaService springerRevistaService;

    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void importaEncabezadoDinamicoContinuaTrasFilaRechazada() throws Exception {
        MockMultipartFile archivo = crearCatalogo();

        when(springerRevistaService.importarFila(any(SpringerRevistaRequestDTO.class)))
                .thenReturn(true)
                .thenReturn(false)
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "ISSN inválido"
                ));

        SpringerImportService service = new SpringerImportService(
                springerRevistaService,
                auditoriaService
        );

        CatalogImportResponseDTO respuesta = service.importar(archivo);

        assertThat(respuesta.getRegistrosLeidos()).isEqualTo(3);
        assertThat(respuesta.getRegistrosInsertados()).isEqualTo(1);
        assertThat(respuesta.getRegistrosActualizados()).isEqualTo(1);
        assertThat(respuesta.getRegistrosRechazados()).isEqualTo(1);
        assertThat(respuesta.getTiempoEjecucionMs()).isGreaterThanOrEqualTo(0);

        verify(springerRevistaService, times(3))
                .importarFila(any(SpringerRevistaRequestDTO.class));
        ArgumentCaptor<SpringerRevistaRequestDTO> captor =
                ArgumentCaptor.forClass(SpringerRevistaRequestDTO.class);
        verify(springerRevistaService, times(3)).importarFila(captor.capture());
        SpringerRevistaRequestDTO primera = captor.getAllValues().get(0);
        assertThat(primera.getNumerosPorVolumen()).isEqualTo(12);
        assertThat(primera.getNumerosProgramados()).isEqualTo(2);
        assertThat(primera.getComentarios()).isEqualTo("Activa");
        verifyNoInteractions(auditoriaService);
    }

    private MockMultipartFile crearCatalogo() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            var hoja = workbook.createSheet("Catálogo");
            hoja.createRow(0).createCell(0)
                    .setCellValue("Springer Nature Journals List");
            hoja.createRow(1).createCell(0)
                    .setCellValue("Versión de prueba");

            var encabezado = hoja.createRow(4);
            encabezado.createCell(0).setCellValue("Title");
            encabezado.createCell(1).setCellValue("Product ID");
            encabezado.createCell(2).setCellValue("ISSN print");
            encabezado.createCell(3).setCellValue("Single Issues per volume");
            encabezado.createCell(4).setCellValue("Scheduled Vol Nos");
            encabezado.createCell(5).setCellValue("Comments");

            var primera = hoja.createRow(5);
            primera.createCell(0).setCellValue("Revista Uno");
            primera.createCell(1).setCellValue("100");
            primera.createCell(2).setCellValue("0378-5955");
            primera.createCell(3).setCellValue(12);
            primera.createCell(4).setCellValue(2);
            primera.createCell(5).setCellValue("Activa");

            var segunda = hoja.createRow(6);
            segunda.createCell(0).setCellValue("Revista Dos");
            segunda.createCell(1).setCellValue("200");

            var tercera = hoja.createRow(7);
            tercera.createCell(0).setCellValue("Revista Inválida");
            tercera.createCell(1).setCellValue("300");
            tercera.createCell(2).setCellValue("0000-0000");

            workbook.write(salida);

            return new MockMultipartFile(
                    "archivo",
                    "catalogo-springer.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    salida.toByteArray()
            );
        }
    }
}
// FIN - Importación Springer
