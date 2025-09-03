package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.Persistable;

/**
 * A InvoicePaymentItem.
 */

@Entity
@Table(name = "invoice_payment_item")
@IdClass(PaymentItemId.class)
public class InvoicePaymentItem implements Persistable<PaymentItemId>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Id
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    @NotNull
    @Column(name = "montant_attendu", nullable = false)
    private Integer amount;

    @NotNull
    @Column(name = "montant_paye", nullable = false)
    private Integer paidAmount;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "third_party_sale_line_id", referencedColumnName = "id"),
        @JoinColumn(name = "third_party_sale_sale_date", referencedColumnName = "sale_date")
    })
    private ThirdPartySaleLine thirdPartySaleLine;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull

    @JoinColumns({
        @JoinColumn(name = "invoice_payment_id", referencedColumnName = "id"),
        @JoinColumn(name = "invoicePayment_transaction_date", referencedColumnName = "transaction_date")
    })
    private InvoicePayment invoicePayment;

    @Transient
    private boolean isNew = true;

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

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

    public PaymentItemId getId() {
        return new PaymentItemId(id, transactionDate);
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

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
