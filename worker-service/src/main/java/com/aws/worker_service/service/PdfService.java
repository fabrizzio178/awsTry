package com.aws.worker_service.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

@Service
public class PdfService {
    public byte[] generarReportePdf(Long taskId, String descripcion){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph titulo = new Paragraph("Reporte de Tarea #" + taskId, fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            document.add(new Paragraph("\n")); 

            document.add(new Paragraph("Descripción de la Tarea:"));
            document.add(new Paragraph(descripcion));

            document.add(new Paragraph("\n")); 
            document.add(new Paragraph("Estado: FINALIZADO"));
            document.add(new Paragraph("Gracias por utilizar nuestro servicio. Generado por Worker Service."));
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }
}