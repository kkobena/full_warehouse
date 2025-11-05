package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;

public interface ReglementDataService {
    void deleteReglement(PaymentId idReglement);

    void deleteReglement(Set<PaymentId> idReglements);

    void printReceipt(PaymentId idReglement);

    List<InvoicePaymentDTO> fetchInvoicesPayments(InvoicePaymentParam invoicePaymentParam);

    List<InvoicePaymentItemDTO> getInvoicePaymentsItems(PaymentId idReglement);

    List<InvoicePaymentDTO> getInvoicePaymentsGroupItems(PaymentId idReglement);

    Resource printToPdf(InvoicePaymentParam invoicePaymentParam) throws ReportFileExportException;

    byte[] generateEscPosReceiptForTauri(PaymentId idReglement) throws IOException;
}
