package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.math.BigDecimal;

public class MvtCaisseSum {

    private BigDecimal amount;
    private String paymentModeCode;
    private String paymentModeLibelle;
    private TypeFinancialTransaction typeTransaction;
    private TransactionTypeAffichage transactionTypeAffichage;
    private String type;

    public MvtCaisseSum() {}

    public BigDecimal getAmount() {
        return amount;
    }

    public MvtCaisseSum setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getPaymentModeCode() {
        return paymentModeCode;
    }

    public MvtCaisseSum setPaymentModeCode(String paymentModeCode) {
        this.paymentModeCode = paymentModeCode;
        return this;
    }

    public String getPaymentModeLibelle() {
        return paymentModeLibelle;
    }

    public MvtCaisseSum setPaymentModeLibelle(String paymentModeLibelle) {
        this.paymentModeLibelle = paymentModeLibelle;
        return this;
    }

    public String getType() {
        return type;
    }

    public MvtCaisseSum setType(String type) {
        this.type = type;
        return this;
    }

    public TransactionTypeAffichage getTransactionTypeAffichage() {
        return transactionTypeAffichage;
    }

    public MvtCaisseSum setTransactionTypeAffichage(TransactionTypeAffichage transactionTypeAffichage) {
        this.transactionTypeAffichage = transactionTypeAffichage;
        return this;
    }

    public TypeFinancialTransaction getTypeTransaction() {
        return typeTransaction;
    }

    public MvtCaisseSum setTypeTransaction(TypeFinancialTransaction typeTransaction) {
        this.typeTransaction = typeTransaction;
        return this;
    }
}
