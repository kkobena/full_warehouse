package com.kobe.warehouse.service.dto;

public class TvaEmbeded {

    private int tva, amount;

    public int getTva() {
        return tva;
    }

    public TvaEmbeded() {}

    public TvaEmbeded setTva(int tva) {
        this.tva = tva;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public TvaEmbeded setAmount(int amount) {
        this.amount = amount;
        return this;
    }
}
