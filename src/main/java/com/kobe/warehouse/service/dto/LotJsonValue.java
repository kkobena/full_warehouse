package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;

public class LotJsonValue {

    @NotNull
    private Long commandeId;

    @NotNull
    private String numLot;

    @NotNull
    private Long receiptItem;

    @NotNull
    private Integer quantityReceived;

    private Integer ugQuantityReceived = 0;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;

    public Integer getUgQuantityReceived() {
        if (Objects.isNull(ugQuantityReceived)) {
            ugQuantityReceived = 0;
        }
        return ugQuantityReceived;
    }

    public LotJsonValue setUgQuantityReceived(Integer ugQuantityReceived) {
        this.ugQuantityReceived = ugQuantityReceived;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public LotJsonValue setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public Long getReceiptItem() {
        return receiptItem;
    }

    public LotJsonValue setReceiptItem(Long receiptItem) {
        this.receiptItem = receiptItem;
        return this;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public LotJsonValue setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
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

    public Long getCommandeId() {
        return commandeId;
    }

    public LotJsonValue setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
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
    public String toString() {
        String sb =
            "LotJsonValue{" +
            "commandeId=" +
            commandeId +
            ", numLot='" +
            numLot +
            '\'' +
            ", receiptItem=" +
            receiptItem +
            ", quantityReceived=" +
            quantityReceived +
            ", ugQuantityReceived=" +
            ugQuantityReceived +
            ", manufacturingDate=" +
            manufacturingDate +
            ", expiryDate=" +
            expiryDate +
            '}';
        return sb;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numLot);
    }
}
