package com.kobe.warehouse.service.cash_register.dto;

import com.kobe.warehouse.domain.CashRegisterItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;

public class CashRegisterItemDTO {

    private long amount;
    private String paymentModeCode;
    private String paymentModeLibelle;
    private TypeFinancialTransaction typeTransaction;

    public CashRegisterItemDTO() {}

    public CashRegisterItemDTO(CashRegisterItem cashRegisterItem) {
        this.amount = cashRegisterItem.getAmount();
        PaymentMode paymentMode = cashRegisterItem.getPaymentMode();
        this.paymentModeCode = paymentMode.getCode();
        this.paymentModeLibelle = paymentMode.getLibelle();
        this.typeTransaction = cashRegisterItem.getTypeFinancialTransaction();
    }

    public TypeFinancialTransaction getTypeTransaction() {
        return typeTransaction;
    }

    public CashRegisterItemDTO setTypeTransaction(TypeFinancialTransaction typeTransaction) {
        this.typeTransaction = typeTransaction;
        return this;
    }

    public long getAmount() {
        return amount;
    }

    public CashRegisterItemDTO setAmount(long amount) {
        this.amount = amount;
        return this;
    }

    public String getPaymentModeCode() {
        return paymentModeCode;
    }

    public CashRegisterItemDTO setPaymentModeCode(String paymentModeCode) {
        this.paymentModeCode = paymentModeCode;
        return this;
    }

    public String getPaymentModeLibelle() {
        return paymentModeLibelle;
    }

    public CashRegisterItemDTO setPaymentModeLibelle(String paymentModeLibelle) {
        this.paymentModeLibelle = paymentModeLibelle;
        return this;
    }
}
