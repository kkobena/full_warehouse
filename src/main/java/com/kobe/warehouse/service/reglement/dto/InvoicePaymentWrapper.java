package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.service.OrganismeDTO;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.util.ArrayList;
import java.util.List;

public class InvoicePaymentWrapper {

    private List<InvoicePaymentDTO> invoicePayments = new ArrayList<>();
    private OrganismeDTO organisme;
    private String periode;

    public String getInvoicePaymentItemsCount() {
        int invoicePaymentItemsCount =
            this.invoicePayments.stream().map(InvoicePaymentDTO::getInvoicePaymentItemsCount).reduce(0, Integer::sum);
        return NumberUtil.formatToString(invoicePaymentItemsCount);
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public OrganismeDTO getOrganisme() {
        return organisme;
    }

    public void setOrganisme(OrganismeDTO organisme) {
        this.organisme = organisme;
    }

    public String getTotalAmount() {
        var totalAmount = this.invoicePayments.stream().map(InvoicePaymentDTO::getTotalAmount).reduce(0, Integer::sum);
        return NumberUtil.formatToString(totalAmount);
    }

    public List<InvoicePaymentDTO> getInvoicePayments() {
        return invoicePayments;
    }

    public void setInvoicePayments(List<InvoicePaymentDTO> invoicePayments) {
        this.invoicePayments = invoicePayments;
    }
}
