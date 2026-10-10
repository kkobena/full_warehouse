package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.util.List;

public interface ReglementReportService {
    byte[] printToPdf(List<InvoicePaymentWrapper> invoicePaymentWrappers) throws ReportFileExportException;

    byte[] printToPdf(InvoicePaymentWrapper invoicePayment) throws ReportFileExportException;
}
