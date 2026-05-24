package com.kobe.warehouse.service.receipt.dto;

public class AssuranceReceiptItem extends SaleReceiptItem {

    private String taux;

    public String getTaux() {
        return taux;
    }

    public void setTaux(String taux) {
        this.taux = taux;
    }

    @Override
    public String toString() {
        return (
            "AssuranceReceiptItem{" +
            "taux='" +
            taux +
            '\'' +
            ", produitName='" +
            produitName +
            '\'' +
            ", quantity='" +
            quantity +
            '\'' +
            ", totalPrice='" +
            totalPrice +
            '\'' +
            ", unitPrice='" +
            unitPrice +
            '\'' +
            '}'
        );
    }
}
