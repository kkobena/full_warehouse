package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;

public class BalanceCaisseDTO {
  private int count;
  private long montantTtc;
  private long montantHt;
  private long montantDiscount;
  private long montantPartAssure;
  private long montantPartAssureur;
  private long montantNet;
  private long montantCash;
  private long montantPaye;
  private long montantCard;
  private long montantMobileMoney;
  private long montantCheck;
  private long montantCredit;
  private long montantDiffere;
  private short typeSalePercent;
  private String typeSale;
  private long panierMoyen;
  private long montantVirement;
  private String modePaiement;
  private String libelleModePaiement;
  private TypeVente typeVente;
  private long montantDepot;
  private long montantAchat;
  private long montantMarge;
  private long amountToBePaid;
  private long amountToBeTakenIntoAccount;
  private long montantNetUg;
  private long montantTtcUg;
  private long montantHtUg;
  private long montantTaxe;
  private long partAssure;
  private long partTiersPayant;
  private TransactionTypeAffichage typeVeTypeAffichage;

  public long getAmountToBePaid() {
    return amountToBePaid;
  }

  public BalanceCaisseDTO setAmountToBePaid(long amountToBePaid) {
    this.amountToBePaid = amountToBePaid;
    return this;
  }

  public long getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public BalanceCaisseDTO setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public long getMontantNetUg() {
    return montantNetUg;
  }

  public BalanceCaisseDTO setMontantNetUg(long montantNetUg) {
    this.montantNetUg = montantNetUg;
    return this;
  }

  public long getMontantTtcUg() {
    return montantTtcUg;
  }

  public BalanceCaisseDTO setMontantTtcUg(long montantTtcUg) {
    this.montantTtcUg = montantTtcUg;
    return this;
  }

  public long getMontantHtUg() {
    return montantHtUg;
  }

  public BalanceCaisseDTO setMontantHtUg(long montantHtUg) {
    this.montantHtUg = montantHtUg;
    return this;
  }

  public long getMontantAchat() {
    return montantAchat;
  }

  public BalanceCaisseDTO setMontantAchat(long montantAchat) {
    this.montantAchat = montantAchat;
    return this;
  }

  public long getMontantMarge() {
    return montantMarge;
  }

  public BalanceCaisseDTO setMontantMarge(long montantMarge) {
    this.montantMarge = montantMarge;
    return this;
  }

  public long getMontantDepot() {
    return montantDepot;
  }

  public BalanceCaisseDTO setMontantDepot(long montantDepot) {
    this.montantDepot = montantDepot;
    return this;
  }

  public long getMontantVirement() {
    return montantVirement;
  }

  public BalanceCaisseDTO setMontantVirement(long montantVirement) {
    this.montantVirement = montantVirement;
    return this;
  }

  public long getMontantPaye() {
    return montantPaye;
  }

  public BalanceCaisseDTO setMontantPaye(long montantPaye) {
    this.montantPaye = montantPaye;
    return this;
  }

  public String getModePaiement() {
    return modePaiement;
  }

  public BalanceCaisseDTO setModePaiement(String modePaiement) {
    this.modePaiement = modePaiement;
    return this;
  }

  public TypeVente getTypeVente() {
    return typeVente;
  }

  public BalanceCaisseDTO setTypeVente(TypeVente typeVente) {
    this.typeVente = typeVente;
    return this;
  }

  public TransactionTypeAffichage getTypeVeTypeAffichage() {
    return typeVeTypeAffichage;
  }

  public BalanceCaisseDTO setTypeVeTypeAffichage(TransactionTypeAffichage typeVeTypeAffichage) {
    this.typeVeTypeAffichage = typeVeTypeAffichage;
    return this;
  }

  public String getLibelleModePaiement() {
    return libelleModePaiement;
  }

  public BalanceCaisseDTO setLibelleModePaiement(String libelleModePaiement) {
    this.libelleModePaiement = libelleModePaiement;
    return this;
  }

  public int getCount() {
    return count;
  }

  public BalanceCaisseDTO setCount(int count) {
    this.count = count;
    return this;
  }

  public long getMontantTtc() {
    return montantTtc;
  }

  public BalanceCaisseDTO setMontantTtc(long montantTtc) {
    this.montantTtc = montantTtc;
    return this;
  }

  public long getMontantHt() {
    return montantHt;
  }

  public BalanceCaisseDTO setMontantHt(long montantHt) {
    this.montantHt = montantHt;
    return this;
  }

  public long getMontantDiscount() {
    return montantDiscount;
  }

  public BalanceCaisseDTO setMontantDiscount(long montantDiscount) {
    this.montantDiscount = montantDiscount;
    return this;
  }

  public long getMontantPartAssure() {
    return montantPartAssure;
  }

  public BalanceCaisseDTO setMontantPartAssure(long montantPartAssure) {
    this.montantPartAssure = montantPartAssure;
    return this;
  }

  public long getMontantPartAssureur() {
    return montantPartAssureur;
  }

  public BalanceCaisseDTO setMontantPartAssureur(long montantPartAssureur) {
    this.montantPartAssureur = montantPartAssureur;
    return this;
  }

  public long getMontantNet() {
    return montantNet;
  }

  public BalanceCaisseDTO setMontantNet(long montantNet) {
    this.montantNet = montantNet;
    return this;
  }

  public long getMontantCash() {
    return montantCash;
  }

  public BalanceCaisseDTO setMontantCash(long montantCash) {
    this.montantCash = montantCash;
    return this;
  }

  public long getMontantCard() {
    return montantCard;
  }

  public BalanceCaisseDTO setMontantCard(long montantCard) {
    this.montantCard = montantCard;
    return this;
  }

  public long getMontantMobileMoney() {
    return montantMobileMoney;
  }

  public BalanceCaisseDTO setMontantMobileMoney(long montantMobileMoney) {
    this.montantMobileMoney = montantMobileMoney;
    return this;
  }

  public long getMontantCheck() {
    return montantCheck;
  }

  public BalanceCaisseDTO setMontantCheck(long montantCheck) {
    this.montantCheck = montantCheck;
    return this;
  }

  public long getMontantCredit() {
    return montantCredit;
  }

  public BalanceCaisseDTO setMontantCredit(long montantCredit) {
    this.montantCredit = montantCredit;
    return this;
  }

  public long getMontantDiffere() {
    return montantDiffere;
  }

  public BalanceCaisseDTO setMontantDiffere(long montantDiffere) {
    this.montantDiffere = montantDiffere;
    return this;
  }

  public short getTypeSalePercent() {
    return typeSalePercent;
  }

  public BalanceCaisseDTO setTypeSalePercent(short typeSalePercent) {
    this.typeSalePercent = typeSalePercent;
    return this;
  }

  public String getTypeSale() {
    return typeSale;
  }

  public BalanceCaisseDTO setTypeSale(String typeSale) {
    this.typeSale = typeSale;
    return this;
  }

  public long getPanierMoyen() {
    return panierMoyen;
  }

  public BalanceCaisseDTO setPanierMoyen(long panierMoyen) {
    this.panierMoyen = panierMoyen;
    return this;
  }

  public long getMontantTaxe() {
    return montantTaxe;
  }

  public BalanceCaisseDTO setMontantTaxe(long montantTaxe) {
    this.montantTaxe = montantTaxe;
    return this;
  }

  public long getPartAssure() {
    return partAssure;
  }

  public BalanceCaisseDTO setPartAssure(long partAssure) {
    this.partAssure = partAssure;
    return this;
  }

  public long getPartTiersPayant() {
    return partTiersPayant;
  }

  public BalanceCaisseDTO setPartTiersPayant(long partTiersPayant) {
    this.partTiersPayant = partTiersPayant;
    return this;
  }
}
