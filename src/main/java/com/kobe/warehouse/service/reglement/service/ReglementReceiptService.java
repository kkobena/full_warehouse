package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;

public interface ReglementReceiptService {
    void printRecipt(InvoicePaymentReceiptDTO invoicePaymentReceipt);
}
