package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PaymentId implements Serializable {
    private Long id;
    private LocalDate transactionDate;

    public PaymentId() {
    }

    public PaymentId(Long id, LocalDate transactionDate) {
        this.id = id;
        this.transactionDate = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId paymentId = (PaymentId) o;
        return Objects.equals(id, paymentId.id) && Objects.equals(
            transactionDate, paymentId.transactionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionDate);
    }
}
