package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.service.dto.records.Tuple;
import java.util.HashSet;
import java.util.Set;

public class BalanceCaisseWrapper {
  private final short typeSalePercent = 100;
  private final Set<BalanceCaisseDTO> balanceCaisses = new HashSet<>();
  private final Set<Tuple> mvtCaisses = new HashSet<>();
  private final Set<Tuple> mvtCaissesByModes = new HashSet<>();
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
  private String periode;
  private Set<BalanceCaisseSum> balanceCaisseSums = new HashSet<>();
  private BalanceCaisseWrapperSum balanceCaisseWrapperSum;

  public short getTypeSalePercent() {
    return typeSalePercent;
  }

  public Set<BalanceCaisseDTO> getBalanceCaisses() {
    return balanceCaisses;
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

  public Set<Tuple> getMvtCaisses() {
    return mvtCaisses;
  }

  public Set<Tuple> getMvtCaissesByModes() {
    return mvtCaissesByModes;
  }

  public String getPeriode() {
    return periode;
  }

  public BalanceCaisseWrapper setPeriode(String periode) {
    this.periode = periode;
    return this;
  }

  public Set<BalanceCaisseSum> getBalanceCaisseSums() {
    return balanceCaisseSums;
  }

  public BalanceCaisseWrapper setBalanceCaisseSums(Set<BalanceCaisseSum> balanceCaisseSums) {
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
