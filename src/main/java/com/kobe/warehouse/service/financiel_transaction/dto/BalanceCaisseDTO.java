package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;

import java.math.BigDecimal;

import static java.util.Objects.isNull;

public class BalanceCaisseDTO {

    private Long count;
    private Long montantTtc;
    private Long montantHt;
    private Integer montantDiscount;
    private Long montantPartAssure;
    private Long montantPartAssureur;
    private Long montantNet;
    private Long montantCash;
    private Long montantPaye;
    private Long montantReel;
    private Long montantCard;
    private Long montantMobileMoney;
    private Long montantCheck;
    private Long montantCredit;
    private Integer montantDiffere;
    private short typeSalePercent;
    private String typeSale;
    private Long panierMoyen;
    private Long montantVirement;
    private String modePaiement;
    private String libelleModePaiement;
    private TypeVente typeVente;
    private Long montantDepot;
    private Long montantAchat;
    private Long montantMarge;
    private Long amountToBePaid;
    private Long amountToBeTakenIntoAccount;
    private Long montantNetUg;
    private Long montantTtcUg;
    private Long montantHtUg;
    private Long montantTaxe;
    private Long partAssure;
    private Long partTiersPayant;
    private TransactionTypeAffichage typeVeTypeAffichage;

    /*
       root.get(Sales_.type),
                cb.count(root.get(Sales_.id)),
                discountExpression,
                montantTtcExpression,
                cb.sumAsLong(payments.get(PaymentTransaction_.paidAmount)),
                cb.sumAsLong(payments.get(PaymentTransaction_.reelAmount)),
                cb.ceiling(
                    cb.sum(
                        cb.quot(
                            montantTtcExpression,
                            cb.sum(1, cb.quot(salesLineSetJoin.get(SalesLine_.taxValue), 100.0d))
                        )
                    )
                ),
                paymentMode.get(PaymentMode_.code),
                paymentMode.get(PaymentMode_.libelle),
                montantTtcAcahtExpression,
                cb.sum(root.get(Sales_.restToPay)),
                cb.sumAsLong(root.get(Sales_.amountToBeTakenIntoAccount)),
                cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partTiersPayant)),
                cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partAssure))
     */
    public BalanceCaisseDTO(String typeSale, Long numberCount, Object discount, Long montantTtc, Long montantPaye, Long montantReel, Double montantHt, String modePaiement, String libelleModePaiement, Long montantAchat, Integer montantDiffere, Long amountToBeTakenIntoAccount, Long partTiersPayant, Long partAssure) {
        this.typeSale = TypeVenteDTO.valueOf(typeSale).getValue();
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

    public BalanceCaisseDTO(Long amount,Long montantSansArrondi, String modePaiement, String libelleModePaiement, TypeFinancialTransaction typeTransaction) {
        this.montantPaye = amount;
        this.modePaiement = modePaiement;
        this.montantReel = montantSansArrondi;
        this.libelleModePaiement = libelleModePaiement;
        this.typeVeTypeAffichage = typeTransaction.getTransactionTypeAffichage();
    }

    public BalanceCaisseDTO() {

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

    public String getTypeSale() {
        return typeSale;
    }

    public BalanceCaisseDTO setTypeSale(String typeSale) {
        this.typeSale = typeSale;
        return this;
    }

    public Long getPanierMoyen() {
        return panierMoyen;
    }

    public BalanceCaisseDTO setPanierMoyen(Long panierMoyen) {
        this.panierMoyen = panierMoyen;
        return this;
    }

    public Long getMontantTaxe() {
        return montantTaxe;
    }

    public BalanceCaisseDTO setMontantTaxe(Long montantTaxe) {
        this.montantTaxe = montantTaxe;
        return this;
    }

    public Long getPartAssure() {
        return partAssure;
    }

    public BalanceCaisseDTO setPartAssure(Long partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public Long getPartTiersPayant() {
        return partTiersPayant;
    }

    public BalanceCaisseDTO setPartTiersPayant(Long partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }
}
