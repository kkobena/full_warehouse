package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FinancialTransactionDTO {

    @NotNull
    private int amount;

    private Long id;
    private LocalDateTime createdAt;

    @NotNull
    private PaymentMode paymentMode;

    private Long organismeId;
    private String ticketCode;
    private LocalDate transactionDate;
    private boolean credit;
    private TypeFinancialTransaction typeFinancialTransaction;

    @NotNull
    private TypeFinancialTransaction typeTransaction;

    private String organismeName;
    private String userFullName;
    private String commentaire;

    public String getUserFullName() {
        return userFullName;
    }

    public FinancialTransactionDTO setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public TypeFinancialTransaction getTypeTransaction() {
        return typeTransaction;
    }

    public FinancialTransactionDTO setTypeTransaction(TypeFinancialTransaction typeTransaction) {
        this.typeTransaction = typeTransaction;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public FinancialTransactionDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public String getOrganismeName() {
        return organismeName;
    }

    public FinancialTransactionDTO setOrganismeName(String organismeName) {
        this.organismeName = organismeName;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public FinancialTransactionDTO setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public Long getId() {
        return id;
    }

    public FinancialTransactionDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public FinancialTransactionDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public FinancialTransactionDTO setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public Long getOrganismeId() {
        return organismeId;
    }

    public FinancialTransactionDTO setOrganismeId(Long organismeId) {
        this.organismeId = organismeId;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public FinancialTransactionDTO setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public FinancialTransactionDTO setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
        return this;
    }

    public boolean isCredit() {
        return credit;
    }

    public FinancialTransactionDTO setCredit(boolean credit) {
        this.credit = credit;
        return this;
    }

    public TypeFinancialTransaction getTypeFinancialTransaction() {
        return typeFinancialTransaction;
    }

    public FinancialTransactionDTO setTypeFinancialTransaction(TypeFinancialTransaction typeFinancialTransaction) {
        this.typeFinancialTransaction = typeFinancialTransaction;
        return this;
    }
}
