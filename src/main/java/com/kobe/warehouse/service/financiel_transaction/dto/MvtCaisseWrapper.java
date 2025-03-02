package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.service.dto.records.Tuple;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MvtCaisseWrapper {

    private List<Tuple> modesPaiementAmounts = new ArrayList<>();
    private List<Tuple> typeTransactionAmounts = new ArrayList<>();
    private BigDecimal totalAmount;
    private BigDecimal debitedAmount;
    private BigDecimal creditedAmount;
    private BigDecimal totalPaymentAmount;
    private BigDecimal totalSaleAmount;
    private BigDecimal totalMobileAmount;

    public MvtCaisseWrapper() {}

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public MvtCaisseWrapper setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public BigDecimal getDebitedAmount() {
        return debitedAmount;
    }

    public MvtCaisseWrapper setDebitedAmount(BigDecimal debitedAmount) {
        this.debitedAmount = debitedAmount;
        return this;
    }

    public BigDecimal getCreditedAmount() {
        return creditedAmount;
    }

    public MvtCaisseWrapper setCreditedAmount(BigDecimal creditedAmount) {
        this.creditedAmount = creditedAmount;
        return this;
    }

    public BigDecimal getTotalPaymentAmount() {
        return totalPaymentAmount;
    }

    public MvtCaisseWrapper setTotalPaymentAmount(BigDecimal totalPaymentAmount) {
        this.totalPaymentAmount = totalPaymentAmount;
        return this;
    }

    public BigDecimal getTotalSaleAmount() {
        return totalSaleAmount;
    }

    public MvtCaisseWrapper setTotalSaleAmount(BigDecimal totalSaleAmount) {
        this.totalSaleAmount = totalSaleAmount;
        return this;
    }

    public BigDecimal getTotalMobileAmount() {
        return totalMobileAmount;
    }

    public MvtCaisseWrapper setTotalMobileAmount(BigDecimal totalMobileAmount) {
        this.totalMobileAmount = totalMobileAmount;
        return this;
    }

    public List<Tuple> getModesPaiementAmounts() {
        return modesPaiementAmounts;
    }

    public MvtCaisseWrapper setModesPaiementAmounts(List<Tuple> modesPaiementAmounts) {
        this.modesPaiementAmounts = modesPaiementAmounts;
        return this;
    }

    public List<Tuple> getTypeTransactionAmounts() {
        return typeTransactionAmounts;
    }

    public MvtCaisseWrapper setTypeTransactionAmounts(List<Tuple> typeTransactionAmounts) {
        this.typeTransactionAmounts = typeTransactionAmounts;
        return this;
    }
}
