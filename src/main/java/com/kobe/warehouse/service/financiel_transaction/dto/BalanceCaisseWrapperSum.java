package com.kobe.warehouse.service.financiel_transaction.dto;

public class BalanceCaisseWrapperSum {
  private long montantCash;
  private long montantCard;
  private long montantMobileMoney;
  private long montantCheck;
  private long montantVirement;
  private long total;

  public long getMontantCash() {
    return montantCash;
  }

  public BalanceCaisseWrapperSum setMontantCash(long montantCash) {
    this.montantCash = montantCash;
    return this;
  }

  public long getMontantCard() {
    return montantCard;
  }

  public BalanceCaisseWrapperSum setMontantCard(long montantCard) {
    this.montantCard = montantCard;
    return this;
  }

  public long getMontantMobileMoney() {
    return montantMobileMoney;
  }

  public BalanceCaisseWrapperSum setMontantMobileMoney(long montantMobileMoney) {
    this.montantMobileMoney = montantMobileMoney;
    return this;
  }

  public long getMontantCheck() {
    return montantCheck;
  }

  public BalanceCaisseWrapperSum setMontantCheck(long montantCheck) {
    this.montantCheck = montantCheck;
    return this;
  }

  public long getMontantVirement() {
    return montantVirement;
  }

  public BalanceCaisseWrapperSum setMontantVirement(long montantVirement) {
    this.montantVirement = montantVirement;
    return this;
  }

  public long getTotal() {
    return total;
  }

  public BalanceCaisseWrapperSum setTotal(long total) {
    this.total = total;
    return this;
  }
}
