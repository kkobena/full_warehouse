package com.kobe.warehouse.service.financiel_transaction.dto;

import java.time.LocalDate;
import java.util.Map;

public class TableauPharmacienDTO {
  private LocalDate mvtDate;
  private long montantComptant;
  private long montantTtc;
  private long montantCredit;
  private long montantRemise;
  private long montantNet;
  private long montantAchat;
  private long montantAchatNet;
  private long montantTaxe;
  private int nombreVente;
  private long montantAvoir;
  private long montantHt;
  private long amountToBePaid;
  private long amountToBeTakenIntoAccount;
  private long montantNetUg;
  private long montantTtcUg;
  private long montantHtUg;
  private long partAssure;
  private Map<Long, AchatDTO> groupAchats;
  private long montantAvoirFournisseur;

  public long getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public TableauPharmacienDTO setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public long getAmountToBePaid() {
    return amountToBePaid;
  }

  public TableauPharmacienDTO setAmountToBePaid(long amountToBePaid) {
    this.amountToBePaid = amountToBePaid;
    return this;
  }

  public LocalDate getMvtDate() {
    return mvtDate;
  }

  public TableauPharmacienDTO setMvtDate(LocalDate mvtDate) {
    this.mvtDate = mvtDate;
    return this;
  }

  public long getMontantNetUg() {
    return montantNetUg;
  }

  public TableauPharmacienDTO setMontantNetUg(long montantNetUg) {
    this.montantNetUg = montantNetUg;
    return this;
  }

  public long getMontantTtcUg() {
    return montantTtcUg;
  }

  public TableauPharmacienDTO setMontantTtcUg(long montantTtcUg) {
    this.montantTtcUg = montantTtcUg;
    return this;
  }

  public long getMontantHtUg() {
    return montantHtUg;
  }

  public TableauPharmacienDTO setMontantHtUg(long montantHtUg) {
    this.montantHtUg = montantHtUg;
    return this;
  }

  public long getPartAssure() {
    return partAssure;
  }

  public TableauPharmacienDTO setPartAssure(long partAssure) {
    this.partAssure = partAssure;
    return this;
  }

  public long getMontantHt() {
    return montantHt;
  }

  public TableauPharmacienDTO setMontantHt(long montantHt) {
    this.montantHt = montantHt;
    return this;
  }

  public long getMontantComptant() {
    return montantComptant;
  }

  public TableauPharmacienDTO setMontantComptant(long montantComptant) {
    this.montantComptant = montantComptant;
    return this;
  }

  public long getMontantTtc() {
    return montantTtc;
  }

  public TableauPharmacienDTO setMontantTtc(long montantTtc) {
    this.montantTtc = montantTtc;
    return this;
  }

  public long getMontantCredit() {
    return montantCredit;
  }

  public TableauPharmacienDTO setMontantCredit(long montantCredit) {
    this.montantCredit = montantCredit;
    return this;
  }

  public long getMontantRemise() {
    return montantRemise;
  }

  public TableauPharmacienDTO setMontantRemise(long montantRemise) {
    this.montantRemise = montantRemise;
    return this;
  }

  public long getMontantNet() {
    return montantNet;
  }

  public TableauPharmacienDTO setMontantNet(long montantNet) {
    this.montantNet = montantNet;
    return this;
  }

  public long getMontantAchat() {
    return montantAchat;
  }

  public TableauPharmacienDTO setMontantAchat(long montantAchat) {
    this.montantAchat = montantAchat;
    return this;
  }

  public long getMontantAchatNet() {
    return montantAchatNet;
  }

  public TableauPharmacienDTO setMontantAchatNet(long montantAchatNet) {
    this.montantAchatNet = montantAchatNet;
    return this;
  }

  public long getMontantTaxe() {
    return montantTaxe;
  }

  public TableauPharmacienDTO setMontantTaxe(long montantTaxe) {
    this.montantTaxe = montantTaxe;
    return this;
  }

  public int getNombreVente() {
    return nombreVente;
  }

  public TableauPharmacienDTO setNombreVente(int nombreVente) {
    this.nombreVente = nombreVente;
    return this;
  }

  public long getMontantAvoir() {
    return montantAvoir;
  }

  public TableauPharmacienDTO setMontantAvoir(long montantAvoir) {
    this.montantAvoir = montantAvoir;
    return this;
  }

  public Map<Long, AchatDTO> getGroupAchats() {
    return groupAchats;
  }

  public TableauPharmacienDTO setGroupAchats(Map<Long, AchatDTO> groupAchats) {
    this.groupAchats = groupAchats;
    return this;
  }

  public long getMontantAvoirFournisseur() {
    return montantAvoirFournisseur;
  }

  public TableauPharmacienDTO setMontantAvoirFournisseur(long montantAvoirFournisseur) {
    this.montantAvoirFournisseur = montantAvoirFournisseur;
    return this;
  }
}
