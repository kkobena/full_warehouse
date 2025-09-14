package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

public class BalanceCaisseDTO {

    private Long count = 0L;
    private Long montantTtc = 0L;
    private Long montantHt = 0L;
    private Integer montantDiscount = 0;
    private Long montantPartAssure = 0L;
    private Long montantPartAssureur = 0L;
    private Long montantNet = 0L;
    private Long montantCash = 0L;
    private Long montantPaye = 0L;
    private Long montantReel = 0L;
    private Long montantCard = 0L;
    private Long montantMobileMoney = 0L;
    private Long montantCheck = 0L;
    private Long montantCredit = 0L;
    private Integer montantDiffere = 0;
    private short typeSalePercent;
    private TypeVenteDTO typeSale;
    private Long panierMoyen = 0L;
    private Long montantVirement = 0L;
    private String modePaiement;
    private String libelleModePaiement;
    private TypeVente typeVente;
    private Long montantDepot = 0L;
    private Long montantAchat = 0L;
    private Long montantMarge = 0L;
    private Long amountToBePaid = 0L;
    private Long amountToBeTakenIntoAccount = 0L;
    private Long montantNetUg = 0L;
    private Long montantTtcUg = 0L;
    private Long montantHtUg = 0L;
    private Long montantTaxe = 0L;
    private Long partAssure = 0L;
    private Long partTiersPayant = 0L;
    private Integer montantRemiseUg = 0;
    private Integer montantRemiseProduit = 0;
    private TransactionTypeAffichage typeVeTypeAffichage;
    private List<PaymentDTO> payments = new ArrayList<>();


    public BalanceCaisseDTO(String typeSale, Long numberCount, Object discount, Long montantTtc, Long montantPaye, Long montantReel, Double montantHt, String modePaiement, String libelleModePaiement, Long montantAchat, Integer montantDiffere, Long amountToBeTakenIntoAccount, Long partTiersPayant, Long partAssure) {
        this.typeSale = TypeVenteDTO.valueOf(typeSale);
        this.count = numberCount;
        this.montantDiscount = getDiscountAsInteger(discount);
        this.montantTtc = montantTtc;
        this.montantPaye = montantPaye;
        this.montantHt = isNull(montantHt) ? null : montantHt.longValue();
        this.montantNet = montantTtc - this.montantDiscount;
        this.modePaiement = modePaiement;
        this.libelleModePaiement = libelleModePaiement;
        this.montantAchat = montantAchat;
        this.montantDiffere = montantDiffere;
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        this.partTiersPayant = partTiersPayant;
        this.partAssure = partAssure;
        this.montantReel = montantReel;
        this.montantTaxe = this.montantTtc - this.montantHt;
    }

    public BalanceCaisseDTO(Long amount, Long montantSansArrondi, String modePaiement, String libelleModePaiement, TypeFinancialTransaction typeTransaction) {
        this.montantPaye = amount;
        this.modePaiement = modePaiement;
        this.montantReel = montantSansArrondi;
        this.libelleModePaiement = libelleModePaiement;
        this.typeVeTypeAffichage = typeTransaction.getTransactionTypeAffichage();
    }

    public BalanceCaisseDTO() {

    }

    public Integer getMontantRemiseUg() {
        return montantRemiseUg;
    }

    public void setMontantRemiseUg(Integer montantRemiseUg) {
        this.montantRemiseUg = montantRemiseUg;
    }

    public Long getMontantReel() {
        return montantReel;
    }

    public void setMontantReel(Long montantReel) {
        this.montantReel = montantReel;
    }

    private Integer getDiscountAsInteger(Object montantDiscount) {
        if (isNull(montantDiscount)) {
            return 0;
        }
        return switch (montantDiscount) {
            case Double m -> m.intValue();
            case Long ml -> ml.intValue();
            case Integer mi -> mi;
            case BigDecimal mb -> mb.intValue();
            default -> 0;
        };
    }

    public Long getAmountToBePaid() {
        return amountToBePaid;
    }

    public BalanceCaisseDTO setAmountToBePaid(Long amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
        return this;
    }

    public Long getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public BalanceCaisseDTO setAmountToBeTakenIntoAccount(Long amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public Long getMontantNetUg() {
        return montantNetUg;
    }

    public BalanceCaisseDTO setMontantNetUg(Long montantNetUg) {
        this.montantNetUg = montantNetUg;
        return this;
    }

    public Long getMontantTtcUg() {
        return montantTtcUg;
    }

    public BalanceCaisseDTO setMontantTtcUg(Long montantTtcUg) {
        this.montantTtcUg = montantTtcUg;
        return this;
    }

    public Long getMontantHtUg() {
        return montantHtUg;
    }

    public BalanceCaisseDTO setMontantHtUg(Long montantHtUg) {
        this.montantHtUg = montantHtUg;
        return this;
    }

    public Long getMontantAchat() {
        return montantAchat;
    }

    public BalanceCaisseDTO setMontantAchat(Long montantAchat) {
        this.montantAchat = montantAchat;
        return this;
    }

    public Long getMontantMarge() {
        montantMarge = getMontantNet() - Objects.requireNonNullElse(montantAchat, 0L);
        return montantMarge;
    }

    public BalanceCaisseDTO setMontantMarge(Long montantMarge) {
        this.montantMarge = montantMarge;
        return this;
    }

    public Long getMontantDepot() {
        return montantDepot;
    }

    public BalanceCaisseDTO setMontantDepot(Long montantDepot) {
        this.montantDepot = montantDepot;
        return this;
    }

    public Long getMontantVirement() {
        return montantVirement;
    }

    public BalanceCaisseDTO setMontantVirement(Long montantVirement) {
        this.montantVirement = montantVirement;
        return this;
    }

    public Long getMontantPaye() {
        return montantPaye;
    }

    public BalanceCaisseDTO setMontantPaye(Long montantPaye) {
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

    public Long getCount() {
        return count;
    }

    public BalanceCaisseDTO setCount(Long count) {
        this.count = count;
        return this;
    }

    public Long getMontantTtc() {
        return montantTtc;
    }

    public BalanceCaisseDTO setMontantTtc(Long montantTtc) {
        this.montantTtc = montantTtc;
        return this;
    }

    public Long getMontantHt() {
        return montantHt;
    }

    public BalanceCaisseDTO setMontantHt(Long montantHt) {
        this.montantHt = montantHt;
        return this;
    }

    public Integer getMontantDiscount() {
        return montantDiscount;
    }

    public BalanceCaisseDTO setMontantDiscount(Integer montantDiscount) {
        this.montantDiscount = montantDiscount;
        return this;
    }

    public Long getMontantPartAssure() {
        return montantPartAssure;
    }

    public BalanceCaisseDTO setMontantPartAssure(Long montantPartAssure) {
        this.montantPartAssure = montantPartAssure;
        return this;
    }

    public Long getMontantPartAssureur() {
        return montantPartAssureur;
    }

    public BalanceCaisseDTO setMontantPartAssureur(Long montantPartAssureur) {
        this.montantPartAssureur = montantPartAssureur;
        return this;
    }

    public Long getMontantNet() {
        montantNet = Objects.requireNonNullElse(montantTtc, 0L) - (Objects.requireNonNullElse(montantDiscount, 0) - Objects.requireNonNullElse(montantRemiseUg, 0));
        return montantNet;
    }

    public BalanceCaisseDTO setMontantNet(Long montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public Long getMontantCash() {
        return montantCash;
    }

    public BalanceCaisseDTO setMontantCash(Long montantCash) {
        this.montantCash = montantCash;
        return this;
    }

    public Long getMontantCard() {
        return montantCard;
    }

    public BalanceCaisseDTO setMontantCard(Long montantCard) {
        this.montantCard = montantCard;
        return this;
    }

    public Long getMontantMobileMoney() {
        return montantMobileMoney;
    }

    public BalanceCaisseDTO setMontantMobileMoney(Long montantMobileMoney) {
        this.montantMobileMoney = montantMobileMoney;
        return this;
    }

    public Long getMontantCheck() {
        return montantCheck;
    }

    public BalanceCaisseDTO setMontantCheck(Long montantCheck) {
        this.montantCheck = montantCheck;
        return this;
    }

    public Long getMontantCredit() {
        return montantCredit;
    }

    public BalanceCaisseDTO setMontantCredit(Long montantCredit) {
        this.montantCredit = montantCredit;
        return this;
    }

    public Integer getMontantDiffere() {
        return montantDiffere;
    }

    public BalanceCaisseDTO setMontantDiffere(Integer montantDiffere) {
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

    public TypeVenteDTO getTypeSale() {
        return typeSale;
    }

    public BalanceCaisseDTO setTypeSale(TypeVenteDTO typeSale) {
        this.typeSale = typeSale;
        return this;
    }

    public Long getPanierMoyen() {
        if (count != 0) {
            panierMoyen = getMontantTtc() / count;
        } else {
            panierMoyen = 0L;
        }
        return panierMoyen;
    }

    public BalanceCaisseDTO setPanierMoyen(Long panierMoyen) {
        this.panierMoyen = panierMoyen;
        return this;
    }

    public Long getMontantTaxe() {
        montantTaxe = Objects.requireNonNullElse(montantTtc, 0L) - Objects.requireNonNullElse(montantHt, 0L);
        return montantTaxe;
    }


    public Long getPartAssure() {
        return partAssure;
    }

    public BalanceCaisseDTO setPartAssure(Long partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public List<PaymentDTO> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDTO> payments) {
        this.payments = payments;
    }

    public Integer getMontantRemiseProduit() {
        return montantRemiseProduit;
    }

    public void setMontantRemiseProduit(Integer montantRemiseProduit) {
        this.montantRemiseProduit = montantRemiseProduit;
    }

    public Long getPartTiersPayant() {
        return partTiersPayant;
    }

    public BalanceCaisseDTO setPartTiersPayant(Long partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }
}
