package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A InvoicePaymentItem.
 */

@Entity
@Table(name = "invoice_payment_item")
public class InvoicePaymentItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "montant_attendu", nullable = false)
    private Integer amount;

    @NotNull
    @Column(name = "montant_paye", nullable = false)
    private Integer paidAmount;

    @NotNull
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_sale_line_id", referencedColumnName = "id")
    private ThirdPartySaleLine thirdPartySaleLine;

    @ManyToOne(optional = false,fetch =  FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "invoice_payment_id", referencedColumnName = "id")
    private InvoicePayment invoicePayment;

    public @NotNull ThirdPartySaleLine getThirdPartySaleLine() {
        return thirdPartySaleLine;
    }

    public InvoicePaymentItem setThirdPartySaleLine(@NotNull ThirdPartySaleLine thirdPartySaleLine) {
        this.thirdPartySaleLine = thirdPartySaleLine;
        return this;
    }

    public @NotNull Integer getPaidAmount() {
        return paidAmount;
    }

    public InvoicePaymentItem setPaidAmount(@NotNull Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public @NotNull InvoicePayment getInvoicePayment() {
        return invoicePayment;
    }

    public InvoicePaymentItem setInvoicePayment(@NotNull InvoicePayment invoicePayment) {
        this.invoicePayment = invoicePayment;
        return this;
    }

    public Long getId() {
        return id;
    }

    public InvoicePaymentItem setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull Integer getAmount() {
        return amount;
    }

    public InvoicePaymentItem setAmount(@NotNull Integer amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InvoicePaymentItem that = (InvoicePaymentItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
