package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.FactureTiersPayant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupeFactureDto {

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
