package com.kobe.warehouse.service.receipt.dto;

import java.util.List;

public abstract class SaleReceiptItem extends AbstractItem {

    protected String produitName;
    protected String quantity;
    protected String unitPrice;
    protected String totalPrice;
    /** Une ligne imprimée par lot prélevé (« Lot A123 (2) exp. 03/2027 ») ; vide sans gestion de lots. */
    protected List<String> lots = List.of();

    public List<String> getLots() {
        return lots;
    }

    public void setLots(List<String> lots) {
        this.lots = lots;
    }

    public String getProduitName() {
        return produitName;
    }

    public void setProduitName(String produitName) {
        this.produitName = produitName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(String totalPrice) {
        this.totalPrice = totalPrice;
    }
}
