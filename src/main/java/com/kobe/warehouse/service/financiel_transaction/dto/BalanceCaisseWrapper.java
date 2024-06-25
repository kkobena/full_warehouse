package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.service.dto.records.Tuple;
import java.util.ArrayList;
import java.util.List;

public class BalanceCaisseWrapper {
  private final short typeSalePercent = 100;
  private List<Tuple> mvtCaissesByModes = new ArrayList<>();
  private List<Tuple> mvtCaisses = new ArrayList<>();
  private List<BalanceCaisseDTO> balanceCaisses = new ArrayList<>();
  private int count;
  private long montantTtc;
  private long montantHt;
  private long montantDiscount;
  private long montantPartAssure;
  private long montantPartAssureur;
  private long montantNet;
  private long montantCash;
  private long montantCard;
  private long montantMobileMoney;
  private long montantCheck;
  private long montantCredit;
  private long montantDiffere;
  private long panierMoyen;
  private long montantVirement;
  private long montantDepot;
  private long montantTaxe;
  private long montantAchat;
  private long montantMarge;
  private long amountToBePaid;
  private long amountToBeTakenIntoAccount;
  private long montantNetUg;
  private long montantTtcUg;
  private long montantHtUg;
  private long partAssure;
  private long partTiersPayant;
  private long montantPaye;
  private String periode;
  private List<BalanceCaisseSum> balanceCaisseSums = new ArrayList<>();
  private BalanceCaisseWrapperSum balanceCaisseWrapperSum;
  private float ratioVenteAchat;
  private float ratioAchatVente;

  public float getRatioVenteAchat() {
    return ratioVenteAchat;
  }

  public BalanceCaisseWrapper setRatioVenteAchat(float ratioVenteAchat) {
    this.ratioVenteAchat = ratioVenteAchat;
    return this;
  }

  public float getRatioAchatVente() {
    return ratioAchatVente;
  }

  public BalanceCaisseWrapper setRatioAchatVente(float ratioAchatVente) {
    this.ratioAchatVente = ratioAchatVente;
    return this;
  }

  public long getMontantTaxe() {
    return montantTaxe;
  }

  public BalanceCaisseWrapper setMontantTaxe(long montantTaxe) {
    this.montantTaxe = montantTaxe;
    return this;
  }

  public long getMontantPaye() {
    return montantPaye;
  }

  public BalanceCaisseWrapper setMontantPaye(long montantPaye) {
    this.montantPaye = montantPaye;
    return this;
  }

  public long getPartAssure() {
    return partAssure;
  }

  public BalanceCaisseWrapper setPartAssure(long partAssure) {
    this.partAssure = partAssure;
    return this;
  }

  public long getPartTiersPayant() {
    return partTiersPayant;
  }

  public BalanceCaisseWrapper setPartTiersPayant(long partTiersPayant) {
    this.partTiersPayant = partTiersPayant;
    return this;
  }

  public long getMontantAchat() {
    return montantAchat;
  }

  public BalanceCaisseWrapper setMontantAchat(long montantAchat) {
    this.montantAchat = montantAchat;
    return this;
  }

  public long getMontantMarge() {
    return montantMarge;
  }

  public BalanceCaisseWrapper setMontantMarge(long montantMarge) {
    this.montantMarge = montantMarge;
    return this;
  }

  public long getAmountToBePaid() {
    return amountToBePaid;
  }

  public BalanceCaisseWrapper setAmountToBePaid(long amountToBePaid) {
    this.amountToBePaid = amountToBePaid;
    return this;
  }

  public long getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public BalanceCaisseWrapper setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public long getMontantNetUg() {
    return montantNetUg;
  }

  public BalanceCaisseWrapper setMontantNetUg(long montantNetUg) {
    this.montantNetUg = montantNetUg;
    return this;
  }

  public long getMontantTtcUg() {
    return montantTtcUg;
  }

  public BalanceCaisseWrapper setMontantTtcUg(long montantTtcUg) {
    this.montantTtcUg = montantTtcUg;
    return this;
  }

  public long getMontantHtUg() {
    return montantHtUg;
  }

  public BalanceCaisseWrapper setMontantHtUg(long montantHtUg) {
    this.montantHtUg = montantHtUg;
    return this;
  }

  public long getMontantDepot() {
    return montantDepot;
  }

  public BalanceCaisseWrapper setMontantDepot(long montantDepot) {
    this.montantDepot = montantDepot;
    return this;
  }

  public short getTypeSalePercent() {
    return typeSalePercent;
  }

  public List<BalanceCaisseDTO> getBalanceCaisses() {
    return balanceCaisses;
  }

  public BalanceCaisseWrapper setBalanceCaisses(List<BalanceCaisseDTO> balanceCaisses) {
    this.balanceCaisses = balanceCaisses;
    return this;
  }

  public int getCount() {
    return count;
  }

  public BalanceCaisseWrapper setCount(int count) {
    this.count = count;
    return this;
  }

  public long getMontantTtc() {
    return montantTtc;
  }

  public BalanceCaisseWrapper setMontantTtc(long montantTtc) {
    this.montantTtc = montantTtc;
    return this;
  }

  public long getMontantHt() {
    return montantHt;
  }

  public BalanceCaisseWrapper setMontantHt(long montantHt) {
    this.montantHt = montantHt;
    return this;
  }

  public long getMontantDiscount() {
    return montantDiscount;
  }

  public BalanceCaisseWrapper setMontantDiscount(long montantDiscount) {
    this.montantDiscount = montantDiscount;
    return this;
  }

  public long getMontantPartAssure() {
    return montantPartAssure;
  }

  public BalanceCaisseWrapper setMontantPartAssure(long montantPartAssure) {
    this.montantPartAssure = montantPartAssure;
    return this;
  }

  public long getMontantPartAssureur() {
    return montantPartAssureur;
  }

  public BalanceCaisseWrapper setMontantPartAssureur(long montantPartAssureur) {
    this.montantPartAssureur = montantPartAssureur;
    return this;
  }

  public long getMontantNet() {
    return montantNet;
  }

  public BalanceCaisseWrapper setMontantNet(long montantNet) {
    this.montantNet = montantNet;
    return this;
  }

  public long getMontantCash() {
    return montantCash;
  }

  public BalanceCaisseWrapper setMontantCash(long montantCash) {
    this.montantCash = montantCash;
    return this;
  }

  public long getMontantCard() {
    return montantCard;
  }

  public BalanceCaisseWrapper setMontantCard(long montantCard) {
    this.montantCard = montantCard;
    return this;
  }

  public long getMontantMobileMoney() {
    return montantMobileMoney;
  }

  public BalanceCaisseWrapper setMontantMobileMoney(long montantMobileMoney) {
    this.montantMobileMoney = montantMobileMoney;
    return this;
  }

  public long getMontantCheck() {
    return montantCheck;
  }

  public BalanceCaisseWrapper setMontantCheck(long montantCheck) {
    this.montantCheck = montantCheck;
    return this;
  }

  public long getMontantCredit() {
    return montantCredit;
  }

  public BalanceCaisseWrapper setMontantCredit(long montantCredit) {
    this.montantCredit = montantCredit;
    return this;
  }

  public long getMontantDiffere() {
    return montantDiffere;
  }

  public BalanceCaisseWrapper setMontantDiffere(long montantDiffere) {
    this.montantDiffere = montantDiffere;
    return this;
  }

  public long getPanierMoyen() {
    return panierMoyen;
  }

  public BalanceCaisseWrapper setPanierMoyen(long panierMoyen) {
    this.panierMoyen = panierMoyen;
    return this;
  }

  public long getMontantVirement() {
    return montantVirement;
  }

  public BalanceCaisseWrapper setMontantVirement(long montantVirement) {
    this.montantVirement = montantVirement;
    return this;
  }

  public List<Tuple> getMvtCaisses() {
    return mvtCaisses;
  }

  public BalanceCaisseWrapper setMvtCaisses(List<Tuple> mvtCaisses) {
    this.mvtCaisses = mvtCaisses;
    return this;
  }

  public List<Tuple> getMvtCaissesByModes() {
    return mvtCaissesByModes;
  }

  public BalanceCaisseWrapper setMvtCaissesByModes(List<Tuple> mvtCaissesByModes) {
    this.mvtCaissesByModes = mvtCaissesByModes;
    return this;
  }

  public String getPeriode() {
    return periode;
  }

  public BalanceCaisseWrapper setPeriode(String periode) {
    this.periode = periode;
    return this;
  }

  public List<BalanceCaisseSum> getBalanceCaisseSums() {
    return balanceCaisseSums;
  }

  public BalanceCaisseWrapper setBalanceCaisseSums(List<BalanceCaisseSum> balanceCaisseSums) {
    this.balanceCaisseSums = balanceCaisseSums;
    return this;
  }

  public BalanceCaisseWrapperSum getBalanceCaisseWrapperSum() {
    return balanceCaisseWrapperSum;
  }

  public BalanceCaisseWrapper setBalanceCaisseWrapperSum(
      BalanceCaisseWrapperSum balanceCaisseWrapperSum) {
    this.balanceCaisseWrapperSum = balanceCaisseWrapperSum;
    return this;
  }
}
