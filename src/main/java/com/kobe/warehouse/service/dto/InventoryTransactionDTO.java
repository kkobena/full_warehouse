package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.User;
import java.time.LocalDateTime;

public class InventoryTransactionDTO {

    private Integer quantity;
    private Integer quantityBefor;
    private Integer quantityAfter;
    private LocalDateTime updatedAt;
    private String transactionType;
    private String produitLibelle;
    private String userFullName;
    private String abbrName;

    public InventoryTransactionDTO() {}

    public InventoryTransactionDTO(InventoryTransaction inventoryTransaction) {
        this.quantity = inventoryTransaction.getQuantity();
        this.quantityBefor = inventoryTransaction.getQuantityBefor();
        this.quantityAfter = inventoryTransaction.getQuantityAfter();
        this.updatedAt = inventoryTransaction.getCreatedAt();
        this.transactionType = inventoryTransaction.getTransactionType().getValue();
        this.produitLibelle = inventoryTransaction.getProduit().getLibelle();
        User user = inventoryTransaction.getUser();
        this.userFullName = user.getFirstName() + " " + user.getLastName();
        this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantityBefor() {
        return quantityBefor;
    }

    public void setQuantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getAbbrName() {
        return abbrName;
    }

    public InventoryTransactionDTO setAbbrName(String abbrName) {
        this.abbrName = abbrName;
        return this;
    }
}
