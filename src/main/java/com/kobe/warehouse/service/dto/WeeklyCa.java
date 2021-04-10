package com.kobe.warehouse.service.dto;

import java.io.Serializable;

public class WeeklyCa implements Serializable {
    private static final long serialVersionUID = 1L;

    private long amount;
    private long numberTransaction;

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getNumberTransaction() {
        return numberTransaction;
    }

    public void setNumberTransaction(long numberTransaction) {
        this.numberTransaction = numberTransaction;
    }

    public WeeklyCa(long amount, long numberTransaction) {
        this.amount = amount;
        this.numberTransaction = numberTransaction;
    }

    public WeeklyCa() {
    }
}
