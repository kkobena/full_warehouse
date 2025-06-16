package com.kobe.warehouse.service.mobile.dto;

import java.util.List;

public class Dashboard {

    private List<KeyValue> sales;
    private List<KeyValue> netAmounts;
    private List<KeyValue> salesTypes;
    private List<KeyValue> counts;
    private List<KeyValue> paymentModes;
    private List<KeyValue> commandes;

    public List<KeyValue> getCommandes() {
        return commandes;
    }

    public void setCommandes(List<KeyValue> commandes) {
        this.commandes = commandes;
    }

    public List<KeyValue> getCounts() {
        return counts;
    }

    public void setCounts(List<KeyValue> counts) {
        this.counts = counts;
    }

    public List<KeyValue> getNetAmounts() {
        return netAmounts;
    }

    public void setNetAmounts(List<KeyValue> netAmounts) {
        this.netAmounts = netAmounts;
    }

    public List<KeyValue> getPaymentModes() {
        return paymentModes;
    }

    public void setPaymentModes(List<KeyValue> paymentModes) {
        this.paymentModes = paymentModes;
    }

    public List<KeyValue> getSales() {
        return sales;
    }

    public void setSales(List<KeyValue> sales) {
        this.sales = sales;
    }

    public List<KeyValue> getSalesTypes() {
        return salesTypes;
    }

    public void setSalesTypes(List<KeyValue> salesTypes) {
        this.salesTypes = salesTypes;
    }
}
