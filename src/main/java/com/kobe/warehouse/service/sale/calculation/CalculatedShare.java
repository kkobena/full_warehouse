package com.kobe.warehouse.service.sale.calculation;

import java.util.Map;

public class CalculatedShare {
    private Long productId;
    private int pharmacyPrice;
    private int calculationBasePrice;
    private double roShare;
    private Map<Long, Double> complementaryShares;
    private double patientShare;
    private double totalReimbursedAmount;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getPharmacyPrice() {
        return pharmacyPrice;
    }

    public void setPharmacyPrice(int pharmacyPrice) {
        this.pharmacyPrice = pharmacyPrice;
    }

    public double getCalculationBasePrice() {
        return calculationBasePrice;
    }

    public void setCalculationBasePrice(int calculationBasePrice) {
        this.calculationBasePrice = calculationBasePrice;
    }

    public double getRoShare() {
        return roShare;
    }

    public void setRoShare(double roShare) {
        this.roShare = roShare;
    }

    public Map<Long, Double> getComplementaryShares() {
        return complementaryShares;
    }

    public void setComplementaryShares(Map<Long, Double> complementaryShares) {
        this.complementaryShares = complementaryShares;
    }

    public double getPatientShare() {
        return patientShare;
    }

    public void setPatientShare(double patientShare) {
        this.patientShare = patientShare;
    }

    public double getTotalReimbursedAmount() {
        return totalReimbursedAmount;
    }

    public void setTotalReimbursedAmount(double totalReimbursedAmount) {
        this.totalReimbursedAmount = totalReimbursedAmount;
    }
}
