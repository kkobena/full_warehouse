package com.kobe.warehouse.web.rest;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class Utils {

    private Utils() {}

    public static ResponseEntity<byte[]> printPDF(byte[] pdfContent, String fileName) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(pdfContent);
    }

    /** CSV en pièce jointe ; l'extension est ajoutée à {@code fileName} s'il ne la porte pas. */
    public static ResponseEntity<byte[]> exportCsv(byte[] csvContent, String fileName) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(fileName.endsWith(".csv") ? fileName : fileName + ".csv").build().toString()
            )
            .body(csvContent);
    }

    public static ResponseEntity<byte[]> exportExcel(byte[] excelContent, String fileName) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(excelContent);
    }
}
