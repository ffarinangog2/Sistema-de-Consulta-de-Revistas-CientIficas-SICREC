package uteq.edu.ec.sicrec.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.dto.ReporteHistorialDTO;
import uteq.edu.ec.sicrec.dto.AuditoriaResponseDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    public byte[] generarReporteHistorialExcel(
            List<ReporteHistorialDTO> datos
    ) throws IOException {

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Historial");

        // ==========================
        // ESTILOS
        // ==========================

        Font tituloFont = workbook.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 16);

        CellStyle tituloStyle = workbook.createCellStyle();
        tituloStyle.setAlignment(HorizontalAlignment.CENTER);
        tituloStyle.setFont(tituloFont);

        Font subtituloFont = workbook.createFont();
        subtituloFont.setBold(true);
        subtituloFont.setFontHeightInPoints((short) 12);

        CellStyle subtituloStyle = workbook.createCellStyle();
        subtituloStyle.setAlignment(HorizontalAlignment.CENTER);
        subtituloStyle.setFont(subtituloFont);

        Font encabezadoFont = workbook.createFont();
        encabezadoFont.setBold(true);
        encabezadoFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle encabezadoStyle = workbook.createCellStyle();
        encabezadoStyle.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );
        encabezadoStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );
        encabezadoStyle.setAlignment(
                HorizontalAlignment.CENTER
        );
        encabezadoStyle.setBorderBottom(BorderStyle.THIN);
        encabezadoStyle.setBorderTop(BorderStyle.THIN);
        encabezadoStyle.setBorderLeft(BorderStyle.THIN);
        encabezadoStyle.setBorderRight(BorderStyle.THIN);
        encabezadoStyle.setFont(encabezadoFont);

        CellStyle datosStyle = workbook.createCellStyle();
        datosStyle.setBorderBottom(BorderStyle.THIN);
        datosStyle.setBorderTop(BorderStyle.THIN);
        datosStyle.setBorderLeft(BorderStyle.THIN);
        datosStyle.setBorderRight(BorderStyle.THIN);

        // ==========================
        // TÍTULO
        // ==========================

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,3));
        Row fila0 = sheet.createRow(0);
        Cell c0 = fila0.createCell(0);
        c0.setCellValue("UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO");
        c0.setCellStyle(tituloStyle);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,3));
        Row fila1 = sheet.createRow(1);
        Cell c1 = fila1.createCell(0);
        c1.setCellValue("Sistema SICREC");
        c1.setCellStyle(subtituloStyle);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,3));
        Row fila2 = sheet.createRow(2);
        Cell c2 = fila2.createCell(0);
        c2.setCellValue("Reporte de Historial de Búsquedas");
        c2.setCellStyle(subtituloStyle);

        Row fila3 = sheet.createRow(3);

        fila3.createCell(0).setCellValue(
                "Fecha de generación:"
        );

        fila3.createCell(1).setCellValue(
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm:ss"
                        )
                )
        );

        // ==========================
        // ENCABEZADOS
        // ==========================

        Row encabezado = sheet.createRow(5);

        String[] columnas = {
                "Fecha",
                "Usuario",
                "Término",
                "Resultados"
        };

        for (int i = 0; i < columnas.length; i++) {

            Cell cell = encabezado.createCell(i);

            cell.setCellValue(columnas[i]);

            cell.setCellStyle(encabezadoStyle);

        }

        // ==========================
        // DATOS
        // ==========================

        int fila = 6;

        for (ReporteHistorialDTO dto : datos) {

            Row row = sheet.createRow(fila++);

            Cell cFecha = row.createCell(0);
            cFecha.setCellValue(dto.getFecha().toString());
            cFecha.setCellStyle(datosStyle);

            Cell cUsuario = row.createCell(1);
            cUsuario.setCellValue(dto.getUsuario());
            cUsuario.setCellStyle(datosStyle);

            Cell cTermino = row.createCell(2);
            cTermino.setCellValue(dto.getTermino());
            cTermino.setCellStyle(datosStyle);

            Cell cResultados = row.createCell(3);
            cResultados.setCellValue(dto.getResultados());
            cResultados.setCellStyle(datosStyle);

        }

        // Autofiltro
        sheet.setAutoFilter(
                new org.apache.poi.ss.util.CellRangeAddress(
                        5,
                        fila - 1,
                        0,
                        3
                )
        );

        // Ajustar columnas
        for (int i = 0; i < 4; i++) {

            sheet.autoSizeColumn(i);

        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        workbook.write(output);

        workbook.close();

        return output.toByteArray();

    }

    // INICIO - Endpoints de auditoría
    public byte[] generarAuditoriaExcel(
            List<AuditoriaResponseDTO> datos
    ) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Auditoría");

        Font encabezadoFont = workbook.createFont();
        encabezadoFont.setBold(true);
        encabezadoFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle encabezadoStyle = workbook.createCellStyle();
        encabezadoStyle.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );
        encabezadoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        encabezadoStyle.setFont(encabezadoFont);

        Row encabezado = sheet.createRow(0);
        String[] columnas = {
                "Fecha y hora",
                "Usuario",
                "Módulo",
                "Acción",
                "Descripción",
                "Dirección IP",
                "Resultado"
        };

        for (int i = 0; i < columnas.length; i++) {
            Cell cell = encabezado.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(encabezadoStyle);
        }

        int fila = 1;

        for (AuditoriaResponseDTO dto : datos) {
            Row row = sheet.createRow(fila++);

            row.createCell(0).setCellValue(
                    dto.getFechaAccion() != null
                            ? dto.getFechaAccion().toString()
                            : ""
            );
            row.createCell(1).setCellValue(dto.getNombreUsuario());
            row.createCell(2).setCellValue(valorSeguro(dto.getModulo()));
            row.createCell(3).setCellValue(valorSeguro(dto.getAccion()));
            row.createCell(4).setCellValue(valorSeguro(dto.getDescripcion()));
            row.createCell(5).setCellValue(valorSeguro(dto.getIpOrigen()));
            row.createCell(6).setCellValue(valorSeguro(dto.getResultado()));
        }

        if (fila > 1) {
            sheet.setAutoFilter(
                    new org.apache.poi.ss.util.CellRangeAddress(
                            0,
                            fila - 1,
                            0,
                            columnas.length - 1
                    )
            );
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        workbook.close();

        return output.toByteArray();
    }

    private String valorSeguro(String valor) {
        return valor != null ? valor : "";
    }
    // FIN - Endpoints de auditoría

}
