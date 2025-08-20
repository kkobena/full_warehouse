package com.kobe.warehouse.service.sale.calculation.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculatedShare {
    private BigDecimal pharmacyPrice;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private Integer calculationBasePrice;
    private double roShare;
    private Map<Long, BigDecimal> tiersPayants= new HashMap<>();
    private BigDecimal patientShare;
    private BigDecimal totalReimbursedAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private List<Rate> rates = new ArrayList<>();
    private Long saleLineId;

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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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

    public double getRoShare() {
        return roShare;
    }

    public void setRoShare(double roShare) {
        this.roShare = roShare;
    }

    public Map<Long, BigDecimal> getTiersPayants() {
        return tiersPayants;
    }

    public void setTiersPayants(Map<Long, BigDecimal> tiersPayants) {
        this.tiersPayants = tiersPayants;
    }

    public BigDecimal getPatientShare() {
        return patientShare;
    }

    public void setPatientShare(BigDecimal patientShare) {
        this.patientShare = patientShare;
    }

    public BigDecimal getTotalReimbursedAmount() {
        return totalReimbursedAmount;
    }

    public Long getSaleLineId() {
        return saleLineId;
    }

    public void setSaleLineId(Long saleLineId) {
        this.saleLineId = saleLineId;
    }

    public void setTotalReimbursedAmount(BigDecimal totalReimbursedAmount) {
        this.totalReimbursedAmount = totalReimbursedAmount;
    }
}
