package com.kobe.warehouse.service.financiel_transaction.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableauPharmacienWrapper {

    private List<FournisseurAchat> groupAchats = new ArrayList<>();
    private List<TableauPharmacienDTO> tableauPharmaciens;
    private float ratioAchatVente;
    private float ratioVenteAchat;
    private long montantAchat;
    private long montantVenteTtc;
    private long montantVenteHt;
    private long montantVenteNet;
    private long montantVenteRemise;
    private long montantVenteTaxe;
    private long numberCount;
    private long montantVenteCredit;
    private long montantVenteComptant;
    private long montantAchatNet;
    private long montantAchatTaxe;
    private long montantAchatRemise;
    private long montantAchatTtc;
    private long montantAchatHt;
    private long montantAvoirFournisseur;
    private Map<Integer, Long> achatFournisseurs = new HashMap<>();

    public List<FournisseurAchat> getGroupAchats() {
        return groupAchats;
    }

    public void setGroupAchats(List<FournisseurAchat> groupAchats) {
        this.groupAchats = groupAchats;
    }

    public Map<Integer, Long> getAchatFournisseurs() {
        return achatFournisseurs;
    }

    public void setAchatFournisseurs(Map<Integer, Long> achatFournisseurs) {
        this.achatFournisseurs = achatFournisseurs;
    }

    public long getMontantAvoirFournisseur() {
        return montantAvoirFournisseur;
    }

    public TableauPharmacienWrapper setMontantAvoirFournisseur(long montantAvoirFournisseur) {
        this.montantAvoirFournisseur = montantAvoirFournisseur;
        return this;
    }

    public long getMontantAchatHt() {
        return montantAchatHt;
    }

    public TableauPharmacienWrapper setMontantAchatHt(long montantAchatHt) {
        this.montantAchatHt = montantAchatHt;
        return this;
    }

    public long getMontantAchat() {
        return montantAchat;
    }

    public TableauPharmacienWrapper setMontantAchat(long montantAchat) {
        this.montantAchat = montantAchat;
        return this;
    }

    public long getMontantVenteTtc() {
        return montantVenteTtc;
    }

    public TableauPharmacienWrapper setMontantVenteTtc(long montantVenteTtc) {
        this.montantVenteTtc = montantVenteTtc;
        return this;
    }

    public long getMontantVenteHt() {
        return montantVenteHt;
    }

    public TableauPharmacienWrapper setMontantVenteHt(long montantVenteHt) {
        this.montantVenteHt = montantVenteHt;
        return this;
    }

    public long getMontantVenteNet() {
        return montantVenteNet;
    }

    public TableauPharmacienWrapper setMontantVenteNet(long montantVenteNet) {
        this.montantVenteNet = montantVenteNet;
        return this;
    }

    public long getMontantVenteRemise() {
        return montantVenteRemise;
    }

    public TableauPharmacienWrapper setMontantVenteRemise(long montantVenteRemise) {
        this.montantVenteRemise = montantVenteRemise;
        return this;
    }

    public long getMontantVenteTaxe() {
        return montantVenteTaxe;
    }

    public TableauPharmacienWrapper setMontantVenteTaxe(long montantVenteTaxe) {
        this.montantVenteTaxe = montantVenteTaxe;
        return this;
    }

    public long getNumberCount() {
        return numberCount;
    }

    public TableauPharmacienWrapper setNumberCount(long numberCount) {
        this.numberCount = numberCount;
        return this;
    }

    public long getMontantVenteCredit() {
        return montantVenteCredit;
    }

    public TableauPharmacienWrapper setMontantVenteCredit(long montantVenteCredit) {
        this.montantVenteCredit = montantVenteCredit;
        return this;
    }

    public long getMontantVenteComptant() {
        return montantVenteComptant;
    }

    public TableauPharmacienWrapper setMontantVenteComptant(long montantVenteComptant) {
        this.montantVenteComptant = montantVenteComptant;
        return this;
    }

    public List<TableauPharmacienDTO> getTableauPharmaciens() {
        return tableauPharmaciens;
    }

    public TableauPharmacienWrapper setTableauPharmaciens(List<TableauPharmacienDTO> tableauPharmaciens) {
        this.tableauPharmaciens = tableauPharmaciens;
        return this;
    }

    public long getMontantAchatNet() {
        return montantAchatNet;
    }

    public TableauPharmacienWrapper setMontantAchatNet(long montantAchatNet) {
        this.montantAchatNet = montantAchatNet;
        return this;
    }

    public long getMontantAchatTaxe() {
        return montantAchatTaxe;
    }

    public TableauPharmacienWrapper setMontantAchatTaxe(long montantAchatTaxe) {
        this.montantAchatTaxe = montantAchatTaxe;
        return this;
    }

    public long getMontantAchatRemise() {
        return montantAchatRemise;
    }

    public TableauPharmacienWrapper setMontantAchatRemise(long montantAchatRemise) {
        this.montantAchatRemise = montantAchatRemise;
        return this;
    }

    public long getMontantAchatTtc() {
        return montantAchatTtc;
    }

    public TableauPharmacienWrapper setMontantAchatTtc(long montantAchatTtc) {
        this.montantAchatTtc = montantAchatTtc;
        return this;
    }

    public TableauPharmacienWrapper addTableauPharmaciens(TableauPharmacienDTO tableauPharmacien) {
        if (this.tableauPharmaciens == null) {
            this.tableauPharmaciens = new ArrayList<>();
        }
        this.tableauPharmaciens.add(tableauPharmacien);
        return this;
    }

    public float getRatioAchatVente() {
        return ratioAchatVente;
    }

    public TableauPharmacienWrapper setRatioAchatVente(float ratioAchatVente) {
        this.ratioAchatVente = ratioAchatVente;
        return this;
    }

    public float getRatioVenteAchat() {
        return ratioVenteAchat;
    }

    public TableauPharmacienWrapper setRatioVenteAchat(float ratioVenteAchat) {
        this.ratioVenteAchat = ratioVenteAchat;
        return this;
    }
}
