package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class AssuranceSaleId implements Serializable {
    private Long id;
    private LocalDate saleDate;

    public AssuranceSaleId() {
    }

    public AssuranceSaleId(Long id, LocalDate saleDate) {
        this.id = id;
        this.saleDate = saleDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssuranceSaleId saleId = (AssuranceSaleId) o;
        return Objects.equals(id, saleId.id) && Objects.equals(saleDate, saleId.saleDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
    }
}
