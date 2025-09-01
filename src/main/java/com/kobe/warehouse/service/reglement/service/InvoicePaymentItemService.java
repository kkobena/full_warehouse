package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentItemService {

    private final IdGeneratorService idGeneratorService;

    public InvoicePaymentItemService(IdGeneratorService idGeneratorService) {
        this.idGeneratorService = idGeneratorService;
        this.idGeneratorService.setSequenceName("id_transaction_item_seq");
    }

    protected InvoicePaymentItem buildInvoicePaymentItem(ThirdPartySaleLine thirdPartySaleLine, InvoicePayment invoicePayment, int amount) {
        InvoicePaymentItem invoicePaymentItem = new InvoicePaymentItem();
        invoicePaymentItem.setId(this.idGeneratorService.nextId());
        invoicePaymentItem.setAmount(thirdPartySaleLine.getMontant() - thirdPartySaleLine.getMontantRegle());
        invoicePaymentItem.setInvoicePayment(invoicePayment);
        invoicePaymentItem.setThirdPartySaleLine(thirdPartySaleLine);
        invoicePaymentItem.setPaidAmount(amount);
        return invoicePaymentItem;
    }
}
