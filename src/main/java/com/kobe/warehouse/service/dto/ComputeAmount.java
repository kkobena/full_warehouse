package com.kobe.warehouse.service.dto;

public class ComputeAmount {
    private Integer payrollAmount;
    private Integer amountToBePaid;
    private Integer discountAmount;
    private Integer salesAmount;
    private Integer amountToBeTakenIntoAccount, montantVerse, montantRendu;
    private Integer htAmount;
    private Integer netAmount;
    private Integer taxAmount;

    public Integer getPayrollAmount() {
        return payrollAmount;
    }

    public ComputeAmount setPayrollAmount(Integer payrollAmount) {
        this.payrollAmount = payrollAmount;
        return this;
    }

    public Integer getAmountToBePaid() {
        return amountToBePaid;
    }

    public ComputeAmount setAmountToBePaid(Integer amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
        return this;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public ComputeAmount setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public ComputeAmount setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public ComputeAmount setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public ComputeAmount setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public Integer getMontantRendu() {
        return montantRendu;
    }

    public ComputeAmount setMontantRendu(Integer montantRendu) {
        this.montantRendu = montantRendu;
        return this;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public ComputeAmount setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public ComputeAmount setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public ComputeAmount setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }
}
