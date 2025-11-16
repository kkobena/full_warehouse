package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.service.id_generator.TransactionItemIdGeneratorService;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentItemService {

    private final TransactionItemIdGeneratorService transactionItemIdGeneratorService;

    public InvoicePaymentItemService(TransactionItemIdGeneratorService transactionItemIdGeneratorService) {
        this.transactionItemIdGeneratorService = transactionItemIdGeneratorService;
    }

    protected InvoicePaymentItem buildInvoicePaymentItem(ThirdPartySaleLine thirdPartySaleLine, InvoicePayment invoicePayment, int amount) {
        InvoicePaymentItem invoicePaymentItem = new InvoicePaymentItem();
        invoicePaymentItem.setId(this.transactionItemIdGeneratorService.nextId());
        invoicePaymentItem.setAmount(thirdPartySaleLine.getMontant() - thirdPartySaleLine.getMontantRegle());
        invoicePaymentItem.setInvoicePayment(invoicePayment);
        invoicePaymentItem.setThirdPartySaleLine(thirdPartySaleLine);
        invoicePaymentItem.setPaidAmount(amount);
        return invoicePaymentItem;
    }
}
