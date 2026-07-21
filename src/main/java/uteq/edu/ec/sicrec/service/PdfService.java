package uteq.edu.ec.sicrec.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.ReporteHistorialDTO;
import uteq.edu.ec.sicrec.dto.AuditoriaResponseDTO;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    public byte[] generarReporteHistorialPDF(
            List<ReporteHistorialDTO> datos
    ) throws Exception {

        Document document = new Document(PageSize.A4);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        PdfWriter.getInstance(document, output);

        document.open();

        Font titulo =
                new Font(Font.HELVETICA, 18, Font.BOLD);

        Font subtitulo =
                new Font(Font.HELVETICA, 13, Font.BOLD);

        Font normal =
                new Font(Font.HELVETICA, 11);

        Paragraph p1 = new Paragraph(
                "UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO",
                titulo
        );

        p1.setAlignment(Element.ALIGN_CENTER);

        document.add(p1);

        Paragraph p2 = new Paragraph(
                "Sistema SICREC",
                subtitulo
        );

        p2.setAlignment(Element.ALIGN_CENTER);

        document.add(p2);

        document.add(new Paragraph(
                "Reporte de Historial de Búsquedas",
                subtitulo
        ));

        document.add(new Paragraph(" "));

        document.add(

                new Paragraph(

                        "Fecha de generación: "

                                + LocalDateTime.now().format(

                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm:ss"
                                )

                        ),

                        normal

                )

        );

        document.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(4);

        tabla.setWidthPercentage(100);

        tabla.addCell(new PdfPCell(new Phrase("Fecha")));
        tabla.addCell(new PdfPCell(new Phrase("Usuario")));
        tabla.addCell(new PdfPCell(new Phrase("Término")));
        tabla.addCell(new PdfPCell(new Phrase("Resultados")));

        for (ReporteHistorialDTO dto : datos) {

            tabla.addCell(dto.getFecha().toString());

            tabla.addCell(dto.getUsuario());

            tabla.addCell(dto.getTermino());

            tabla.addCell(
                    String.valueOf(dto.getResultados())
            );

        }

        document.add(tabla);

        document.close();

        return output.toByteArray();

    }

    // INICIO - Endpoints de auditoría
    public byte[] generarAuditoriaPDF(
            List<AuditoriaResponseDTO> datos
    ) throws Exception {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, output);
        document.open();

        Font titulo = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph encabezado = new Paragraph(
                "Auditoría general - SICREC",
                titulo
        );
        encabezado.setAlignment(Element.ALIGN_CENTER);
        document.add(encabezado);
        document.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);

        String[] columnas = {
                "Fecha",
                "Usuario",
                "Módulo",
                "Acción",
                "Descripción",
                "IP",
                "Resultado"
        };

        for (String columna : columnas) {
            tabla.addCell(new PdfPCell(new Phrase(columna)));
        }

        for (AuditoriaResponseDTO dto : datos) {
            tabla.addCell(
                    dto.getFechaAccion() != null
                            ? dto.getFechaAccion().toString()
                            : ""
            );
            tabla.addCell(dto.getNombreUsuario());
            tabla.addCell(valorSeguro(dto.getModulo()));
            tabla.addCell(valorSeguro(dto.getAccion()));
            tabla.addCell(valorSeguro(dto.getDescripcion()));
            tabla.addCell(valorSeguro(dto.getIpOrigen()));
            tabla.addCell(valorSeguro(dto.getResultado()));
        }

        document.add(tabla);
        document.close();

        return output.toByteArray();
    }

    private String valorSeguro(String valor) {
        return valor != null ? valor : "";
    }
    // FIN - Endpoints de auditoría

}
