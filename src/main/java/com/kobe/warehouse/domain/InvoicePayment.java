package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A InvoicePayment.
 */

@Entity
public class InvoicePayment extends PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne
    private FactureTiersPayant factureTiersPayant;

    @OneToMany(mappedBy = "invoicePayment", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<InvoicePaymentItem> invoicePaymentItems = new ArrayList<>();

    @ManyToOne
    private InvoicePayment parent;



    private boolean grouped;

    @OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<InvoicePayment> invoicePayments = new ArrayList<>();



    public FactureTiersPayant getFactureTiersPayant() {
        return factureTiersPayant;
    }

    public InvoicePayment setFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        this.factureTiersPayant = factureTiersPayant;
        return this;
    }

    public List<InvoicePaymentItem> getInvoicePaymentItems() {
        return invoicePaymentItems;
    }

    public InvoicePayment setInvoicePaymentItems(List<InvoicePaymentItem> invoicePaymentItems) {
        this.invoicePaymentItems = invoicePaymentItems;
        return this;
    }

    public InvoicePayment getParent() {
        return parent;
    }

    public InvoicePayment setParent(InvoicePayment parent) {
        this.parent = parent;
        return this;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public InvoicePayment setGrouped(boolean grouped) {
        this.grouped = grouped;
        return this;
    }

    public List<InvoicePayment> getInvoicePayments() {
        return invoicePayments;
    }

    public InvoicePayment setInvoicePayments(List<InvoicePayment> invoicePayments) {
        this.invoicePayments = invoicePayments;
        return this;
    }
}
