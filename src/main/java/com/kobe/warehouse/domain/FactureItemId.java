package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class FactureItemId implements Serializable {

    private Long id;
    private LocalDate invoiceDate;

    public FactureItemId() {}

    public FactureItemId(Long id, LocalDate invoiceDate) {
        this.id = id;
        this.invoiceDate = invoiceDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FactureItemId paymentId = (FactureItemId) o;
        return Objects.equals(id, paymentId.id) && Objects.equals(invoiceDate, paymentId.invoiceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, invoiceDate);
    }
}
