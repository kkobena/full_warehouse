package com.kobe.warehouse.service.financiel_transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.isNull;

public class TableauPharmacienDTO {

    private LocalDate mvtDate;
    private long montantComptant;
    private int montantDiffere;
    private long montantTtc;
    private long montantCredit;
    private long montantRemise;
    private long  montantReel;
    private long montantNet;
    private long montantAchat;
    private long montantAchatNet;
    private long montantTaxe;
    private long nombreVente;
    private long montantAvoir;
    private long montantHt;
    private long amountToBePaid;
    private long amountToBeTakenIntoAccount;
    private long montantNetUg;
    private long montantTtcUg;
    private long montantHtUg;
    private long partAssure;
    private long montantRemiseUg;
    private List<FournisseurAchat> groupAchats = new ArrayList<>();
    private long montantAvoirFournisseur;
    private long montantBonAchat;
    private float ratioAchatVente;
    private float ratioVenteAchat;
    private Map<Long, Long> achatFournisseurs = new HashMap<>();
    private List<PaymentDTO> payments = new ArrayList<>();

    public Map<Long, Long> getAchatFournisseurs() {
        return achatFournisseurs;
    }

    public void setAchatFournisseurs(Map<Long, Long> achatFournisseurs) {
        this.achatFournisseurs = achatFournisseurs;
    }

    public long getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public TableauPharmacienDTO setAmountToBeTakenIntoAccount(long amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }



    public int getMontantDiffere() {
        return montantDiffere;
    }

    public void setMontantDiffere(int montantDiffere) {
        this.montantDiffere = montantDiffere;
    }

    public float getRatioAchatVente() {
        return ratioAchatVente;
    }

    public TableauPharmacienDTO setRatioAchatVente(float ratioAchatVente) {
        this.ratioAchatVente = ratioAchatVente;
        return this;
    }

    public long getMontantRemiseUg() {
        return montantRemiseUg;
    }

    public void setMontantRemiseUg(long montantRemiseUg) {
        this.montantRemiseUg = montantRemiseUg;
    }

    public float getRatioVenteAchat() {
        return ratioVenteAchat;
    }

    public TableauPharmacienDTO setRatioVenteAchat(float ratioVenteAchat) {
        this.ratioVenteAchat = ratioVenteAchat;
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

    public long getNombreVente() {
        return nombreVente;
    }



    public long getMontantAvoir() {
        return montantAvoir;
    }

    public TableauPharmacienDTO setMontantAvoir(long montantAvoir) {
        this.montantAvoir = montantAvoir;
        return this;
    }

    public List<FournisseurAchat> getGroupAchats() {
        return groupAchats;
    }

    public TableauPharmacienDTO setGroupAchats(List<FournisseurAchat> groupAchats) {
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

    public long getMontantBonAchat() {
        return montantBonAchat;
    }

    public TableauPharmacienDTO setMontantBonAchat(long montantBonAchat) {
        this.montantBonAchat = montantBonAchat;
        return this;
    }
    private Integer getDiscountAsInteger(Object montantDiscount) {
        if (isNull(montantDiscount)) {
            return 0;
        }
        if (montantDiscount instanceof Double) {
            return ((Double) montantDiscount).intValue();
        } else if (montantDiscount instanceof Long) {
            return ((Long) montantDiscount).intValue();
        } else if (montantDiscount instanceof Integer) {
            return (Integer) montantDiscount;
        } else if (montantDiscount instanceof BigDecimal) {
            return ((BigDecimal) montantDiscount).intValue();
        }
        return 0;
    }

    public long getMontantReel() {
        return montantReel;
    }

    public void setMontantReel(long montantReel) {
        this.montantReel = montantReel;
    }

    public void setNombreVente(long nombreVente) {
        this.nombreVente = nombreVente;
    }

    public TableauPharmacienDTO() {
    }

    public List<PaymentDTO> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDTO> payments) {
        this.payments = payments;
    }

    public TableauPharmacienDTO(LocalDate mvtDate, Long numberCount, Object discount, Long montantTtc, Long montantPaye, Long montantReel, Double montantHt, Long montantAchat, Integer montantDiffere, Long amountToBeTakenIntoAccount, Long partTiersPayant, Long partAssure) {
        this.mvtDate = mvtDate;
        this.nombreVente = numberCount;
        this.montantRemise = getDiscountAsInteger(discount);
        this.montantTtc = montantTtc;
        this.montantComptant = montantPaye;
        this.montantHt = isNull(montantHt) ? null : montantHt.longValue();
        this.montantNet = montantTtc - this.montantRemise;
        this.montantAchat = montantAchat;
        this.montantCredit = Objects .requireNonNullElse(partTiersPayant,0L) + Objects.requireNonNullElse(montantDiffere,0) ;
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        this.partAssure = partAssure;
        this.montantReel = montantReel;
        this.montantTaxe = this.montantTtc - this.montantHt;
    }
}
