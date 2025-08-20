package com.kobe.warehouse.service.sale.calculation.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SaleItemInput {
    //private Long produitId;
    private Long salesLineId;
    private Integer quantity;
    private BigDecimal regularUnitPrice;
    private BigDecimal discountAmount= BigDecimal.ZERO;
    private List<TiersPayantPrixInput> prixAssurances = new ArrayList<>();
    private BigDecimal totalSalesAmount;
    public Long getSalesLineId() {
        return salesLineId;
    }

    public void setSalesLineId(Long salesLineId) {
        this.salesLineId = salesLineId;
    }

/*    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }*/

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(BigDecimal regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public List<TiersPayantPrixInput> getPrixAssurances() {
        return prixAssurances;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setPrixAssurances(List<TiersPayantPrixInput> prixAssurances) {
        this.prixAssurances = prixAssurances;
    }
}
