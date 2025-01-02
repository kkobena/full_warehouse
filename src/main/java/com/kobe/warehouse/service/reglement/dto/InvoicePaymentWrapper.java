package com.kobe.warehouse.service.reglement.dto;

import java.util.ArrayList;
import java.util.List;

public class InvoicePaymentWrapper {

    private final List<InvoicePaymentDTO> invoicePayments = new ArrayList<>();
    private String organisme;

    public List<InvoicePaymentDTO> getInvoicePayments() {
        return invoicePayments;
    }

    public String getOrganisme() {
        return organisme;
    }

    public void setOrganisme(String organisme) {
        this.organisme = organisme;
    }
}
