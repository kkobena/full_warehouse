package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;

public class LotJsonValue {

    @NotNull
    private String numLot;

    private int quantity;
    private int freeQuantity;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private long linkedId; // id of the entry line or order line

    public int getFreeQuantity() {
        return freeQuantity;
    }

    public LotJsonValue setFreeQuantity(int freeQuantity) {
        this.freeQuantity = freeQuantity;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public LotJsonValue setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public int getQuantity() {
        return quantity;
    }

    public LotJsonValue setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public LotJsonValue setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
        return this;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LotJsonValue setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public long getLinkedId() {
        return linkedId;
    }

    public LotJsonValue setLinkedId(long linkedId) {
        this.linkedId = linkedId;
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
        LotJsonValue that = (LotJsonValue) o;
        return numLot.equals(that.numLot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numLot);
    }
}
