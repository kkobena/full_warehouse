package com.kobe.warehouse.service.sale.calculation.dto;

import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculatedShare {

    private BigDecimal pharmacyPrice;
    private BigDecimal totalSalesAmount = BigDecimal.ZERO;
    private Integer calculationBasePrice;
    private Map<Long, BigDecimal> tiersPayants = new HashMap<>();
    private BigDecimal totalReimbursedAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private List<Rate> rates = new ArrayList<>();
    private Long saleLineId;

    public boolean hasPriceOption() {
        return !rates.isEmpty() || nonNull(calculationBasePrice);
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public List<Rate> getRates() {
        return rates;
    }

    public void setRates(List<Rate> rates) {
        this.rates = rates;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public BigDecimal getPharmacyPrice() {
        return pharmacyPrice;
    }

    public void setPharmacyPrice(BigDecimal pharmacyPrice) {
        this.pharmacyPrice = pharmacyPrice;
    }

    public Integer getCalculationBasePrice() {
        return calculationBasePrice;
    }

    public void setCalculationBasePrice(Integer calculationBasePrice) {
        this.calculationBasePrice = calculationBasePrice;
    }

    public Map<Long, BigDecimal> getTiersPayants() {
        return tiersPayants;
    }

    public void setTiersPayants(Map<Long, BigDecimal> tiersPayants) {
        this.tiersPayants = tiersPayants;
    }

    public BigDecimal getTotalReimbursedAmount() {
        return totalReimbursedAmount;
    }

    public void setTotalReimbursedAmount(BigDecimal totalReimbursedAmount) {
        this.totalReimbursedAmount = totalReimbursedAmount;
    }

    public Long getSaleLineId() {
        return saleLineId;
    }

    public void setSaleLineId(Long saleLineId) {
        this.saleLineId = saleLineId;
    }
}
