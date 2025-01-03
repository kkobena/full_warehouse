package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;

public interface ReglementDataService {
    void deleteReglement(long idReglement);

    void deleteReglement(Set<Long> idReglements);

    void printReceipt(long idReglement);

    List<InvoicePaymentWrapper> getInvoicePayments(InvoicePaymentParam invoicePaymentParam);

    List<InvoicePaymentDTO> fetchInvoicesPayments(InvoicePaymentParam invoicePaymentParam);

    List<InvoicePaymentItemDTO> getInvoicePaymentsItems(long idReglement);

    List<InvoicePaymentDTO> getInvoicePaymentsGroupItems(long idReglement);

    Resource printToPdf(InvoicePaymentParam invoicePaymentParam) throws ReportFileExportException;
}
