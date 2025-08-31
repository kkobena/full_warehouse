package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DifferePaymentItem.
 */

@Entity
@Table(name = "differe_payment_item")
public class DifferePaymentItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "expected_amount", nullable = false)
    private Integer expectedAmount;

    @NotNull
    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @NotNull
    @ManyToOne(optional = false)
    private Sales sale;

    @ManyToOne(optional = false,fetch =  FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "differe_payment_id", referencedColumnName = "id")
    private DifferePayment differePayment;

    public Long getId() {
        return id;
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
}
