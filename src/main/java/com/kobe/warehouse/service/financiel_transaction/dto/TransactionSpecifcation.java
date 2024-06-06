package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionSpecifcation {
  private Long saleId;
  private Long transactionId;
  private Integer paidAmount;
  private String customerFullName;
  private String typeVente;
  private String salePaymentModeCode;
  private String salePaymentModeLibelle;
  private String userFullName;
  private LocalDateTime saleDate;
  private String saleNumberTransaction;
  private String saleNumBon;
  private LocalDate workDay;
  private SalesStatut saleStatut;
  private String saleCa;
  private Integer paymentTransactionAmount;
  private LocalDateTime paymentTransactionAmountCreated;
  private Integer typeTransaction;
  private String transactionUserFullName;
  private String transactionCustomerFullName;
  private String tiersPayantName;
  private String transactionPaymentModeLibelle;
  private String transactionPaymentModeCode;
  private Long saleUserId;
  private String paymentTransactionTicketCode;
  private Integer transactionCategorieCa;

  public TransactionSpecifcation() {}

  public Long getSaleId() {
    return saleId;
  }

  public TransactionSpecifcation setSaleId(Long saleId) {
    this.saleId = saleId;
    return this;
  }

  public Long getTransactionId() {
    return transactionId;
  }

  public TransactionSpecifcation setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public Integer getPaidAmount() {
    return paidAmount;
  }

  public TransactionSpecifcation setPaidAmount(Integer paidAmount) {
    this.paidAmount = paidAmount;
    return this;
  }

  public String getCustomerFullName() {
    return customerFullName;
  }

  public TransactionSpecifcation setCustomerFullName(String customerFullName) {
    this.customerFullName = customerFullName;
    return this;
  }

  public String getTypeVente() {
    return typeVente;
  }

  public TransactionSpecifcation setTypeVente(String typeVente) {
    this.typeVente = typeVente;
    return this;
  }

  public String getSalePaymentModeCode() {
    return salePaymentModeCode;
  }

  public TransactionSpecifcation setSalePaymentModeCode(String salePaymentModeCode) {
    this.salePaymentModeCode = salePaymentModeCode;
    return this;
  }

  public String getSalePaymentModeLibelle() {
    return salePaymentModeLibelle;
  }

  public TransactionSpecifcation setSalePaymentModeLibelle(String salePaymentModeLibelle) {
    this.salePaymentModeLibelle = salePaymentModeLibelle;
    return this;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public TransactionSpecifcation setUserFullName(String userFullName) {
    this.userFullName = userFullName;
    return this;
  }

  public LocalDateTime getSaleDate() {
    return saleDate;
  }

  public TransactionSpecifcation setSaleDate(LocalDateTime saleDate) {
    this.saleDate = saleDate;
    return this;
  }

  public String getSaleNumberTransaction() {
    return saleNumberTransaction;
  }

  public TransactionSpecifcation setSaleNumberTransaction(String saleNumberTransaction) {
    this.saleNumberTransaction = saleNumberTransaction;
    return this;
  }

  public String getSaleNumBon() {
    return saleNumBon;
  }

  public TransactionSpecifcation setSaleNumBon(String saleNumBon) {
    this.saleNumBon = saleNumBon;
    return this;
  }

  public LocalDate getWorkDay() {
    return workDay;
  }

  public TransactionSpecifcation setWorkDay(LocalDate workDay) {
    this.workDay = workDay;
    return this;
  }

  public SalesStatut getSaleStatut() {
    return saleStatut;
  }

  public TransactionSpecifcation setSaleStatut(SalesStatut saleStatut) {
    this.saleStatut = saleStatut;
    return this;
  }

  public String getSaleCa() {
    return saleCa;
  }

  public TransactionSpecifcation setSaleCa(String saleCa) {
    this.saleCa = saleCa;
    return this;
  }

  public Integer getPaymentTransactionAmount() {
    return paymentTransactionAmount;
  }

  public TransactionSpecifcation setPaymentTransactionAmount(Integer paymentTransactionAmount) {
    this.paymentTransactionAmount = paymentTransactionAmount;
    return this;
  }

  public LocalDateTime getPaymentTransactionAmountCreated() {
    return paymentTransactionAmountCreated;
  }

  public TransactionSpecifcation setPaymentTransactionAmountCreated(
      LocalDateTime paymentTransactionAmountCreated) {
    this.paymentTransactionAmountCreated = paymentTransactionAmountCreated;
    return this;
  }

  public Integer getTypeTransaction() {
    return typeTransaction;
  }

  public TransactionSpecifcation setTypeTransaction(Integer typeTransaction) {
    this.typeTransaction = typeTransaction;
    return this;
  }

  public String getTransactionUserFullName() {
    return transactionUserFullName;
  }

  public TransactionSpecifcation setTransactionUserFullName(String transactionUserFullName) {
    this.transactionUserFullName = transactionUserFullName;
    return this;
  }

  public String getTransactionCustomerFullName() {
    return transactionCustomerFullName;
  }

  public TransactionSpecifcation setTransactionCustomerFullName(
      String transactionCustomerFullName) {
    this.transactionCustomerFullName = transactionCustomerFullName;
    return this;
  }

  public String getTiersPayantName() {
    return tiersPayantName;
  }

  public TransactionSpecifcation setTiersPayantName(String tiersPayantName) {
    this.tiersPayantName = tiersPayantName;
    return this;
  }

  public String getTransactionPaymentModeLibelle() {
    return transactionPaymentModeLibelle;
  }

  public TransactionSpecifcation setTransactionPaymentModeLibelle(
      String transactionPaymentModeLibelle) {
    this.transactionPaymentModeLibelle = transactionPaymentModeLibelle;
    return this;
  }

  public String getTransactionPaymentModeCode() {
    return transactionPaymentModeCode;
  }

  public TransactionSpecifcation setTransactionPaymentModeCode(String transactionPaymentModeCode) {
    this.transactionPaymentModeCode = transactionPaymentModeCode;
    return this;
  }

  public Long getSaleUserId() {
    return saleUserId;
  }

  public TransactionSpecifcation setSaleUserId(Long saleUserId) {
    this.saleUserId = saleUserId;
    return this;
  }

  public String getPaymentTransactionTicketCode() {
    return paymentTransactionTicketCode;
  }

  public TransactionSpecifcation setPaymentTransactionTicketCode(
      String paymentTransactionTicketCode) {
    this.paymentTransactionTicketCode = paymentTransactionTicketCode;
    return this;
  }

  public Integer getTransactionCategorieCa() {
    return transactionCategorieCa;
  }

  public TransactionSpecifcation setTransactionCategorieCa(Integer transactionCategorieCa) {
    this.transactionCategorieCa = transactionCategorieCa;
    return this;
  }
}
