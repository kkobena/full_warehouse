package com.kobe.warehouse.domain;

import java.time.LocalDate;
import java.util.Objects;

public class OrderLineLot {

    private int quantity;
    private int freeQuantity;
    private String lotNumber;
    private LocalDate expirationDate;
    private LocalDate manufacturingDate;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(int freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public OrderLineLot(int quantity, int freeQuantity, String lotNumber, LocalDate expirationDate, LocalDate manufacturingDate) {
        this.quantity = quantity;
        this.freeQuantity = freeQuantity;
        this.lotNumber = lotNumber;
        this.expirationDate = expirationDate;
        this.manufacturingDate = manufacturingDate;
    }

    public OrderLineLot() {}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderLineLot that = (OrderLineLot) o;
        return Objects.equals(lotNumber, that.lotNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lotNumber);
    }
}
