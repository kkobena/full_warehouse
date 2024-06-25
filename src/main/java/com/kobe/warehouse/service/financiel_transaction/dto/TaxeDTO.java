package com.kobe.warehouse.service.financiel_transaction.dto;

import java.time.LocalDate;

public class TaxeDTO {
  private LocalDate mvtDate;
  private long montantHt;
  private long montantTaxe;
  private long montantTtc;
  private long montantNet;
  private long montantRemise;
  private long montantAchat;
  private long montantRemiseUg;
  private long montantTvaUg;
  private int codeTva;
  private long amountToBeTakenIntoAccount;
  private long montantTtcUg;

  public LocalDate getMvtDate() {
    return mvtDate;
  }

  public TaxeDTO setMvtDate(LocalDate mvtDate) {
    this.mvtDate = mvtDate;
    return this;
  }

  public long getMontantHt() {
    return montantHt;
  }

  public TaxeDTO setMontantHt(long montantHt) {
    this.montantHt = montantHt;
    return this;
  }

  public long getMontantTaxe() {
    return montantTaxe;
  }

  public TaxeDTO setMontantTaxe(long montantTaxe) {
    this.montantTaxe = montantTaxe;
    return this;
  }

  public long getMontantTtc() {
    return montantTtc;
  }

  public TaxeDTO setMontantTtc(long montantTtc) {
    this.montantTtc = montantTtc;
    return this;
  }

  public long getMontantNet() {
    return montantNet;
  }

  public TaxeDTO setMontantNet(long montantNet) {
    this.montantNet = montantNet;
    return this;
  }

  public long getMontantRemise() {
    return montantRemise;
  }

  public TaxeDTO setMontantRemise(long montantRemise) {
    this.montantRemise = montantRemise;
    return this;
  }

  public long getMontantAchat() {
    return montantAchat;
  }

  public TaxeDTO setMontantAchat(long montantAchat) {
    this.montantAchat = montantAchat;
    return this;
  }

  public long getMontantRemiseUg() {
    return montantRemiseUg;
  }

  public TaxeDTO setMontantRemiseUg(long montantRemiseUg) {
    this.montantRemiseUg = montantRemiseUg;
    return this;
  }

  public long getMontantTvaUg() {
    return montantTvaUg;
  }

  public TaxeDTO setMontantTvaUg(long montantTvaUg) {
    this.montantTvaUg = montantTvaUg;
    return this;
  }

  public int getCodeTva() {
    return codeTva;
  }

  public TaxeDTO setCodeTva(int codeTva) {
    this.codeTva = codeTva;
    return this;
  }

  public long getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public TaxeDTO setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public long getMontantTtcUg() {
    return montantTtcUg;
  }

  public TaxeDTO setMontantTtcUg(long montantTtcUg) {
    this.montantTtcUg = montantTtcUg;
    return this;
  }
}
