package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;

public interface ReglementReportService {
    void printRecipt(InvoicePaymentReceiptDTO invoicePaymentReceipt);
}
