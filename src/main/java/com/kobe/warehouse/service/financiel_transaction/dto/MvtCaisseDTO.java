package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.util.Objects;

public class MvtCaisseDTO {
  private String reference;
  private String date;
  private String heure;
  private long montant;
  private TypeFinancialTransaction type;
  private String transactionType;
  private String organisme;
  private String userFullName;
  private String ticketCode;
  private String paymentMode;
  private String paymentModeLibelle;
  private Long id;
  private String numBon;
  private CategorieChiffreAffaire categorieChiffreAffaire;
  private SalesStatut statut;
  private String transactionDate;
  private int netAmount;
  private int htAmount;
  private Integer partAssure;
  private Integer partAssureur;
  private int discount;

  public MvtCaisseDTO() {}

  public SalesStatut getStatut() {
    return statut;
  }

  public MvtCaisseDTO setStatut(SalesStatut statut) {
    this.statut = statut;
    return this;
  }

  public String getTransactionType() {
    if (Objects.nonNull(type)) {
      transactionType = type.getValue();
    }
    return transactionType;
  }

  public MvtCaisseDTO setTransactionType(String transactionType) {
    this.transactionType = transactionType;
    return this;
  }

  public String getTransactionDate() {
    return transactionDate;
  }

  public MvtCaisseDTO setTransactionDate(String transactionDate) {
    this.transactionDate = transactionDate;
    return this;
  }

  public int getNetAmount() {
    return netAmount;
  }

  public MvtCaisseDTO setNetAmount(int netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public int getHtAmount() {
    return htAmount;
  }

  public MvtCaisseDTO setHtAmount(int htAmount) {
    this.htAmount = htAmount;
    return this;
  }

  public Integer getPartAssure() {
    return partAssure;
  }

  public MvtCaisseDTO setPartAssure(Integer partAssure) {
    this.partAssure = partAssure;
    return this;
  }

  public Integer getPartAssureur() {
    return partAssureur;
  }

  public MvtCaisseDTO setPartAssureur(Integer partAssureur) {
    this.partAssureur = partAssureur;
    return this;
  }

  public int getDiscount() {
    return discount;
  }

  public MvtCaisseDTO setDiscount(int discount) {
    this.discount = discount;
    return this;
  }

  public String getPaymentModeLibelle() {
    return paymentModeLibelle;
  }

  public MvtCaisseDTO setPaymentModeLibelle(String paymentModeLibelle) {
    this.paymentModeLibelle = paymentModeLibelle;
    return this;
  }

  public String getNumBon() {
    return numBon;
  }

  public MvtCaisseDTO setNumBon(String numBon) {
    this.numBon = numBon;
    return this;
  }

  public String getReference() {
    return reference;
  }

  public MvtCaisseDTO setReference(String reference) {
    this.reference = reference;
    return this;
  }

  public String getDate() {
    return date;
  }

  public MvtCaisseDTO setDate(String date) {
    this.date = date;
    return this;
  }

  public String getHeure() {
    return heure;
  }

  public MvtCaisseDTO setHeure(String heure) {
    this.heure = heure;
    return this;
  }

  public long getMontant() {
    return montant;
  }

  public MvtCaisseDTO setMontant(long montant) {
    this.montant = montant;
    return this;
  }

  public TypeFinancialTransaction getType() {
    return type;
  }

  public MvtCaisseDTO setType(TypeFinancialTransaction type) {
    this.type = type;
    return this;
  }

  public String getOrganisme() {
    return organisme;
  }

  public MvtCaisseDTO setOrganisme(String organisme) {
    this.organisme = organisme;
    return this;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public MvtCaisseDTO setUserFullName(String userFullName) {
    this.userFullName = userFullName;
    return this;
  }

  public String getTicketCode() {
    return ticketCode;
  }

  public MvtCaisseDTO setTicketCode(String ticketCode) {
    this.ticketCode = ticketCode;
    return this;
  }

  public String getPaymentMode() {
    return paymentMode;
  }

  public MvtCaisseDTO setPaymentMode(String paymentMode) {
    this.paymentMode = paymentMode;
    return this;
  }

  public CategorieChiffreAffaire getCategorieChiffreAffaire() {
    return categorieChiffreAffaire;
  }

  public MvtCaisseDTO setCategorieChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
    this.categorieChiffreAffaire = categorieChiffreAffaire;
    return this;
  }

  public Long getId() {
    return id;
  }

  public MvtCaisseDTO setId(Long id) {
    this.id = id;
    return this;
  }
}
