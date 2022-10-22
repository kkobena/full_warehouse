package com.kobe.warehouse.service.dto;

import java.io.Serializable;

public class StatistiqueProduit implements Serializable {
    private static final long serialVersionUID = 1L;

    private long amount;
    private long quantity;
    private String libelleProduit;

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public String getLibelleProduit() {
        return libelleProduit;
    }

    public void setLibelleProduit(String libelleProduit) {
        this.libelleProduit = libelleProduit;
    }

    public StatistiqueProduit(long quantity, String libelleProduit) {

        this.quantity = quantity;
        this.libelleProduit = libelleProduit;
    }

    public StatistiqueProduit(String libelleProduit, long amount) {
        this.amount = amount;
        this.libelleProduit = libelleProduit;
    }

    public StatistiqueProduit() {
    }
}
