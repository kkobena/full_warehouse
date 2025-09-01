package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
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
 * A DifferePaymentItem.
 */

@Entity
@Table(name = "differe_payment_item")
@IdClass(DiffereItemId.class)
public class DifferePaymentItem implements Persistable<DiffereItemId>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Id
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    @NotNull
    @Column(name = "expected_amount", nullable = false)
    private Integer expectedAmount;

    @NotNull
    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @NotNull
    @ManyToOne(optional = false)
    private Sales sale;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "differe_payment_id", referencedColumnName = "id")
    private DifferePayment differePayment;

    @Transient
    private boolean isNew = true;

    public DiffereItemId getId() {
        return new DiffereItemId(id, transactionDate);
    }

    public DifferePaymentItem setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getExpectedAmount() {
        return expectedAmount;
    }

    public DifferePaymentItem setExpectedAmount(Integer expectedAmount) {
        this.expectedAmount = expectedAmount;
        return this;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public DifferePaymentItem setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public Sales getSale() {
        return sale;
    }

    public DifferePaymentItem setSale(Sales sale) {
        this.sale = sale;
        return this;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public DifferePayment getDifferePayment() {
        return differePayment;
    }

    public DifferePaymentItem setDifferePayment(DifferePayment differePayment) {
        this.differePayment = differePayment;
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
        DifferePaymentItem that = (DifferePaymentItem) o;
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
