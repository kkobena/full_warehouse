package com.kobe.warehouse.service.errors;

public class ReportFileExportException extends BadRequestAlertException {

    public ReportFileExportException() {
        super("Error lors génération du fichier");
    }
}
