package com.kobe.warehouse.service.sale.calculation.dto;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import java.math.BigDecimal;
import java.util.List;

public class CalculationInput {
    private List<SaleItemInput> saleItems;
    private List<TiersPayantInput> tiersPayants;
    private NatureVente natureVente;
    private BigDecimal discountAmount;
    private BigDecimal totalSalesAmount;

    // Getters and Setters
    public List<SaleItemInput> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItemInput> saleItems) {
        this.saleItems = saleItems;
    }

    public List<TiersPayantInput> getTiersPayants() {
        return tiersPayants;
    }

    public void setTiersPayants(List<TiersPayantInput> tiersPayants) {
        this.tiersPayants = tiersPayants;
    }

    public NatureVente getNatureVente() {
        return natureVente;
    }

    public void setNatureVente(NatureVente natureVente) {
        this.natureVente = natureVente;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }
}
