package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class SaleId  implements Serializable {
    private Long id;
    private LocalDate saleDate;

    public SaleId() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public SaleId(Long id, LocalDate saleDate) {
        this.id = id;
        this.saleDate = saleDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SaleId saleId = (SaleId) o;
        return Objects.equals(id, saleId.id) && Objects.equals(saleDate, saleId.saleDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
    }
}
