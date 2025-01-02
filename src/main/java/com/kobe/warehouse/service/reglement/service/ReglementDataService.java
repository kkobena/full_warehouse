package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.util.List;

public interface ReglementDataService {
    void deleteReglement(long idReglement);

    void printReceipt(long idReglement);

    List<InvoicePaymentWrapper> getInvoicePayments(InvoicePaymentParam invoicePaymentParam);

    List<InvoicePaymentDTO> fetchInvoicesPayments(InvoicePaymentParam invoicePaymentParam);

    List<InvoicePaymentItemDTO> getInvoicePaymentsItems(long idReglement);

    List<InvoicePaymentDTO> getInvoicePaymentsGroupItems(long idReglement);
}
