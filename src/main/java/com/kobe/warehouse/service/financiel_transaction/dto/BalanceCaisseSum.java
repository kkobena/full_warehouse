package com.kobe.warehouse.service.financiel_transaction.dto;

public class BalanceCaisseSum {
  private String libelleTypeMvt;
  private String typeMvt;
  private long montantCash;
  private long montantCard;
  private long montantMobileMoney;
  private long montantCheck;
  private long montantVirement;
  private long total;

  public String getLibelleTypeMvt() {
    return libelleTypeMvt;
  }

  public BalanceCaisseSum setLibelleTypeMvt(String libelleTypeMvt) {
    this.libelleTypeMvt = libelleTypeMvt;
    return this;
  }

  public String getTypeMvt() {
    return typeMvt;
  }

  public BalanceCaisseSum setTypeMvt(String typeMvt) {
    this.typeMvt = typeMvt;
    return this;
  }

  public long getMontantCash() {
    return montantCash;
  }

  public BalanceCaisseSum setMontantCash(long montantCash) {
    this.montantCash = montantCash;
    return this;
  }

  public long getMontantCard() {
    return montantCard;
  }

  public BalanceCaisseSum setMontantCard(long montantCard) {
    this.montantCard = montantCard;
    return this;
  }

  public long getMontantMobileMoney() {
    return montantMobileMoney;
  }

  public BalanceCaisseSum setMontantMobileMoney(long montantMobileMoney) {
    this.montantMobileMoney = montantMobileMoney;
    return this;
  }

  public long getMontantCheck() {
    return montantCheck;
  }

  public BalanceCaisseSum setMontantCheck(long montantCheck) {
    this.montantCheck = montantCheck;
    return this;
  }

  public long getMontantVirement() {
    return montantVirement;
  }

  public BalanceCaisseSum setMontantVirement(long montantVirement) {
    this.montantVirement = montantVirement;
    return this;
  }

  public long getTotal() {
    return total;
  }

  public BalanceCaisseSum setTotal(long total) {
    this.total = total;
    return this;
  }
}
