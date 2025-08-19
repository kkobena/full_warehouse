package com.kobe.warehouse.service.sale.calculation;

import java.util.ArrayList;
import java.util.List;

public class CalculationResult {
    private List<CalculatedShare> itemShares = new ArrayList<>();
    private int totalRoShare;
    private int totalComplementaryShare;
    private int totalPatientShare;
    private int totalSaleAmount;

    public List<CalculatedShare> getItemShares() {
        return itemShares;
    }

    public void setItemShares(List<CalculatedShare> itemShares) {
        this.itemShares = itemShares;
    }

    public int getTotalRoShare() {
        return totalRoShare;
    }

    public void setTotalRoShare(int totalRoShare) {
        this.totalRoShare = totalRoShare;
    }

    public int getTotalComplementaryShare() {
        return totalComplementaryShare;
    }

    public void setTotalComplementaryShare(int totalComplementaryShare) {
        this.totalComplementaryShare = totalComplementaryShare;
    }

    public int getTotalPatientShare() {
        return totalPatientShare;
    }

    public void setTotalPatientShare(int totalPatientShare) {
        this.totalPatientShare = totalPatientShare;
    }

    public int getTotalSaleAmount() {
        return totalSaleAmount;
    }

    public void setTotalSaleAmount(int totalSaleAmount) {
        this.totalSaleAmount = totalSaleAmount;
    }

    public void add(CalculatedShare itemShare) {
        this.itemShares.add(itemShare);
    }

    public void aggregateTotals() {
        this.totalRoShare =(int)Math.ceil(itemShares.stream().mapToDouble(CalculatedShare::getRoShare).sum()) ;
        this.totalComplementaryShare = (int)Math.ceil(itemShares.stream().mapToDouble(s -> s.getComplementaryShares().values().stream().mapToDouble(Double::doubleValue).sum()).sum());
        this.totalPatientShare = (int)Math.ceil(itemShares.stream().mapToDouble(CalculatedShare::getPatientShare).sum());
        this.totalSaleAmount = (int)Math.ceil(itemShares.stream().mapToDouble(CalculatedShare::getPharmacyPrice).sum());
    }
}
