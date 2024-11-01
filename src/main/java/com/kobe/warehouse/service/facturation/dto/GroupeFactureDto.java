package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.FactureTiersPayant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupeFactureDto extends FactureDtoWrapper {

    private LocalDate debutPeriode;
    private LocalDate finPeriode;
    private String telephone;
    private String name;
    private String adresse;
    private long montant;
    private int itemsBonCount;
    private String numFacture;
    private List<FactureDto> factures = new ArrayList<>();
    private String invoiceTotalAmountLetters;
    private long invoiceTotalAmount;
    private LocalDateTime created;
    private List<FactureTiersPayant> facturesTiersPayants = new ArrayList<>();
    private long montantRegle;
    private long remiseForfetaire;
    private long montantVente;
    private long montantRemiseVente;
    private long montantNetVente;
    private long montantNet;
    private long montantAttendu;

    public long getMontantNet() {
        return montantNet;
    }

    public GroupeFactureDto setMontantNet(long montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public long getMontantAttendu() {
        return montantAttendu;
    }

    public GroupeFactureDto setMontantAttendu(long montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public long getMontantNetVente() {
        return montantNetVente;
    }

    public GroupeFactureDto setMontantNetVente(long montantNetVente) {
        this.montantNetVente = montantNetVente;
        return this;
    }

    public long getMontantRegle() {
        return montantRegle;
    }

    public GroupeFactureDto setMontantRegle(long montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    public long getMontantRemiseVente() {
        return montantRemiseVente;
    }

    public GroupeFactureDto setMontantRemiseVente(long montantRemiseVente) {
        this.montantRemiseVente = montantRemiseVente;
        return this;
    }

    public long getMontantVente() {
        return montantVente;
    }

    public GroupeFactureDto setMontantVente(long montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public long getRemiseForfetaire() {
        return remiseForfetaire;
    }

    public GroupeFactureDto setRemiseForfetaire(long remiseForfetaire) {
        this.remiseForfetaire = remiseForfetaire;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public GroupeFactureDto setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getAdresse() {
        return adresse;
    }

    public GroupeFactureDto setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public LocalDate getDebutPeriode() {
        return debutPeriode;
    }

    public GroupeFactureDto setDebutPeriode(LocalDate debutPeriode) {
        this.debutPeriode = debutPeriode;
        return this;
    }

    public LocalDate getFinPeriode() {
        return finPeriode;
    }

    public GroupeFactureDto setFinPeriode(LocalDate finPeriode) {
        this.finPeriode = finPeriode;
        return this;
    }

    public List<FactureTiersPayant> getFacturesTiersPayants() {
        return facturesTiersPayants;
    }

    public GroupeFactureDto setFacturesTiersPayants(List<FactureTiersPayant> facturesTiersPayants) {
        this.facturesTiersPayants = facturesTiersPayants;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public GroupeFactureDto setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public List<FactureDto> getFactures() {
        return factures;
    }

    public GroupeFactureDto setFactures(List<FactureDto> factures) {
        this.factures = factures;
        return this;
    }

    public String getName() {
        return name;
    }

    public GroupeFactureDto setName(String name) {
        this.name = name;
        return this;
    }

    public long getInvoiceTotalAmount() {
        return invoiceTotalAmount;
    }

    public GroupeFactureDto setInvoiceTotalAmount(long invoiceTotalAmount) {
        this.invoiceTotalAmount = invoiceTotalAmount;
        return this;
    }

    public String getInvoiceTotalAmountLetters() {
        return invoiceTotalAmountLetters;
    }

    public GroupeFactureDto setInvoiceTotalAmountLetters(String invoiceTotalAmountLetters) {
        this.invoiceTotalAmountLetters = invoiceTotalAmountLetters;
        return this;
    }

    public int getItemsBonCount() {
        return itemsBonCount;
    }

    public GroupeFactureDto setItemsBonCount(int itemsBonCount) {
        this.itemsBonCount = itemsBonCount;
        return this;
    }

    public long getMontant() {
        return montant;
    }

    public GroupeFactureDto setMontant(long montant) {
        this.montant = montant;
        return this;
    }

    public String getNumFacture() {
        return numFacture;
    }

    public GroupeFactureDto setNumFacture(String numFacture) {
        this.numFacture = numFacture;
        return this;
    }
}
