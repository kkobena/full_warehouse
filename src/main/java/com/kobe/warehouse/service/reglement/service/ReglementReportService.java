package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.util.List;
import org.springframework.core.io.Resource;

public interface ReglementReportService {
    Resource printToPdf(List<InvoicePaymentWrapper> invoicePaymentWrappers) throws ReportFileExportException;

    Resource printToPdf(InvoicePaymentWrapper invoicePayment) throws ReportFileExportException;
}
