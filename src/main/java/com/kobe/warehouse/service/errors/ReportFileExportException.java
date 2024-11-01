package com.kobe.warehouse.service.errors;

import org.springframework.http.HttpStatus;

public class ReportFileExportException extends BadRequestAlertException {

    public ReportFileExportException() {
        super(HttpStatus.BAD_REQUEST, "Error lors génération du fichier");
    }
}
