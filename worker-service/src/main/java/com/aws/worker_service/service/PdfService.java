package com.aws.worker_service.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color; // Ojo, usa java.awt para colores en OpenPDF
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generarReportePdf(Long taskId, String descripcion) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // encabezado/header
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.DARK_GRAY);
            Paragraph titulo = new Paragraph("REPORTE DE PROCESAMIENTO", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            
            Font fontFecha = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Paragraph subtitulo = new Paragraph("Generado el: " + fechaActual, fontFecha);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(30);
            document.add(subtitulo);

            // detalles
            PdfPTable table = new PdfPTable(2); // 2 columnas
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            table.setWidths(new float[]{3f, 7f});

            // helper para celdas
            addTableHeader(table, "Campo");
            addTableHeader(table, "Valor");
            
            addRow(table, "ID de Tarea", "#" + taskId);
            addRow(table, "Descripción", descripcion);
            addRow(table, "Estado Final", "COMPLETED");
            addRow(table, "Worker Node", System.getenv().getOrDefault("HOSTNAME", "Unknown"));

            document.add(table);

            // footer
            document.add(new Paragraph("\n"));
            Font fontFooter = FontFactory.getFont(FontFactory.COURIER_OBLIQUE, 10, Color.LIGHT_GRAY);
            Paragraph footer = new Paragraph("Este documento fue generado automáticamente por una arquitectura de microservicios orientada a eventos en AWS (simulado).", fontFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(Color.LIGHT_GRAY);
        header.setBorderWidth(2);
        header.setPhrase(new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        header.setPadding(8);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        cellLabel.setPadding(8);
        cellLabel.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 12)));
        cellValue.setPadding(8);
        cellValue.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cellValue);
    }
}