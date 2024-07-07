package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.service.dto.DoughnutChart;
import java.util.ArrayList;
import java.util.List;

public class TaxeWrapperDTO {
  private boolean groupDate;
  private long montantHt;
  private long montantTaxe;
  private long montantTtc;
  private long montantNet;
  private long montantRemise;
  private long montantAchat;
  private long montantRemiseUg;
  private long montantTvaUg;
  private long amountToBeTakenIntoAccount;
  private long montantTtcUg;
  private List<TaxeDTO> taxes = new ArrayList<>();
  private DoughnutChart chart;

  public DoughnutChart getChart() {
    return chart;
  }

  public TaxeWrapperDTO setChart(DoughnutChart chart) {
    this.chart = chart;
    return this;
  }

  public boolean isGroupDate() {
    return groupDate;
  }

  public TaxeWrapperDTO setGroupDate(boolean groupDate) {
    this.groupDate = groupDate;
    return this;
  }

  public List<TaxeDTO> getTaxes() {
    return taxes;
  }

  public TaxeWrapperDTO setTaxes(List<TaxeDTO> taxes) {
    this.taxes = taxes;
    return this;
  }

  public long getMontantHt() {
    return montantHt;
  }

  public TaxeWrapperDTO setMontantHt(long montantHt) {
    this.montantHt = montantHt;
    return this;
  }

  public long getMontantTaxe() {
    return montantTaxe;
  }

  public TaxeWrapperDTO setMontantTaxe(long montantTaxe) {
    this.montantTaxe = montantTaxe;
    return this;
  }

  public long getMontantTtc() {
    return montantTtc;
  }

  public TaxeWrapperDTO setMontantTtc(long montantTtc) {
    this.montantTtc = montantTtc;
    return this;
  }

  public long getMontantNet() {
    return montantNet;
  }

  public TaxeWrapperDTO setMontantNet(long montantNet) {
    this.montantNet = montantNet;
    return this;
  }

  public long getMontantRemise() {
    return montantRemise;
  }

  public TaxeWrapperDTO setMontantRemise(long montantRemise) {
    this.montantRemise = montantRemise;
    return this;
  }

  public long getMontantAchat() {
    return montantAchat;
  }

  public TaxeWrapperDTO setMontantAchat(long montantAchat) {
    this.montantAchat = montantAchat;
    return this;
  }

  public long getMontantRemiseUg() {
    return montantRemiseUg;
  }

  public TaxeWrapperDTO setMontantRemiseUg(long montantRemiseUg) {
    this.montantRemiseUg = montantRemiseUg;
    return this;
  }

  public long getMontantTvaUg() {
    return montantTvaUg;
  }

  public TaxeWrapperDTO setMontantTvaUg(long montantTvaUg) {
    this.montantTvaUg = montantTvaUg;
    return this;
  }

  public long getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public TaxeWrapperDTO setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public long getMontantTtcUg() {
    return montantTtcUg;
  }

  public TaxeWrapperDTO setMontantTtcUg(long montantTtcUg) {
    this.montantTtcUg = montantTtcUg;
    return this;
  }
}
