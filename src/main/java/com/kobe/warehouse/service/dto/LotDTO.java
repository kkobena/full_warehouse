package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class LotDTO {

    private Integer id;

    private String numLot;

    private String receiptReference;

    private OrderLineId receiptItemId;

    private Integer quantity;

    private Integer freeQty;

    private Integer quantityReceived;

    private Integer ugQuantityReceived;

    private LocalDateTime createdDate;

    private LocalDate manufacturingDate;

    private LocalDate expiryDate;

    private List<LotSoldDTO> lotSolds;

    private Integer currentQuantity;
    /** Numéro de série FMD (AI 21 GS1 DataMatrix). Null si scan 1D ou non présent. */
    private String serialNumber;
    /** Id du produit — utilisé pour la saisie de lot hors commande (sans OrderLine). */
    private Integer produitId;
    /** Storage cible (optionnel) — si absent, le storage principal de l'utilisateur connecté est utilisé. */
    private Integer storageId;

    public LotDTO(Lot lot) {
        id = lot.getId();
        numLot = lot.getNumLot();
        freeQty = lot.getFreeQty();
        ugQuantityReceived = lot.getFreeQty();
        quantity = lot.getQuantity();
        quantityReceived = lot.getQuantity();
        currentQuantity = lot.getCurrentQuantity();
        createdDate = lot.getCreatedDate();
        manufacturingDate = lot.getManufacturingDate();
        expiryDate = lot.getExpiryDate();
        serialNumber = lot.getSerialNumber();
    }

    public LotDTO() {}

    public Integer getId() {
        return id;
    }

    public LotDTO setId(Integer id) {
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

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public LotDTO setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
        return this;
    }

    public List<LotSoldDTO> getLotSolds() {
        return lotSolds;
    }

    public LotDTO setLotSolds(List<LotSoldDTO> lotSolds) {
        this.lotSolds = lotSolds;
        return this;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public LotDTO setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public LotDTO setProduitId(Integer produitId) {
        this.produitId = produitId;
        return this;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public LotDTO setStorageId(Integer storageId) {
        this.storageId = storageId;
        return this;
    }

    public Integer getUgQuantityReceived() {
        return ugQuantityReceived;
    }

    public LotDTO setUgQuantityReceived(Integer ugQuantityReceived) {
        this.ugQuantityReceived = ugQuantityReceived;
        return this;
    }

    public Lot toEntity() {
        var ug = Optional.ofNullable(ugQuantityReceived).orElse(Optional.ofNullable(freeQty).orElse(0));
        return new Lot()
            .setNumLot(numLot)
            .setExpiryDate(expiryDate)
            .setManufacturingDate(manufacturingDate)
            .setSerialNumber(serialNumber)
            .setQuantity(quantityReceived + ug)
            .setStatut(StatutLot.IN_PROGRESS)
            .setFreeQty(ug);
    }
}
