package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class LotDTO {

    private Long id;

    private String numLot;

    private String receiptReference;

    private OrderLineId receiptItemId;

    private Integer quantity;

    private Integer freeQty;

    private Integer quantityReceived;

    private LocalDateTime createdDate;

    private LocalDate manufacturingDate;

    private LocalDate expiryDate;

    private List<LotSoldDTO> lotSolds;

    public LotDTO(Lot lot) {
        id = lot.getId();
        numLot = lot.getNumLot();
        freeQty = lot.getFreeQty();
        quantity = lot.getQuantity();
        quantityReceived = lot.getQuantity();
        createdDate = lot.getCreatedDate();
        manufacturingDate = lot.getManufacturingDate();
        expiryDate = lot.getExpiryDate();
    }

    public LotDTO() {}

    public Long getId() {
        return id;
    }

    public LotDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public LotDTO setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public LotDTO setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
        return this;
    }

    public OrderLineId getReceiptItemId() {
        return receiptItemId;
    }

    public LotDTO setReceiptItemId(OrderLineId receiptItemId) {
        this.receiptItemId = receiptItemId;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LotDTO setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Integer getFreeQty() {
        return freeQty;
    }

    public LotDTO setFreeQty(Integer freeQty) {
        this.freeQty = freeQty;
        return this;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public LotDTO setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LotDTO setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public LotDTO setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
        return this;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LotDTO setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public List<LotSoldDTO> getLotSolds() {
        return lotSolds;
    }

    public LotDTO setLotSolds(List<LotSoldDTO> lotSolds) {
        this.lotSolds = lotSolds;
        return this;
    }

    public Lot toEntity() {
        var ug = Optional.ofNullable(freeQty).orElse(0);
        return new Lot()
            .setNumLot(numLot)
            .setExpiryDate(expiryDate)
            .setManufacturingDate(manufacturingDate)
            .setQuantity(quantityReceived + ug)
            .setStatut(StatutLot.AVAILABLE)
            .setFreeQty(ug);
    }
}
