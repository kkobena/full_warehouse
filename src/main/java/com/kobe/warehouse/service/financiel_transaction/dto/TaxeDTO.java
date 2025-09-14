package com.kobe.warehouse.service.financiel_transaction.dto;

import java.time.LocalDate;
import java.util.Objects;

import static java.util.Objects.isNull;

public class TaxeDTO {

    private LocalDate mvtDate;
    private Long montantHt = 0L;
    private Long montantTaxe = 0L;
    private Long montantTtc = 0L;
    private Long montantNet = 0L;
    private long montantRemise;
    private Long montantAchat = 0L;
    private Integer codeTva;
    private Long amountToBeTakenIntoAccount = 0L;

    public TaxeDTO() {
    }

    public TaxeDTO(LocalDate mvtDate, Integer codeTva, Long montantTtc, Long montantAchat, Double montantHt, Long amountToBeTakenIntoAccount) {
        this.mvtDate = mvtDate;
        this.codeTva = codeTva;
        this.montantTtc = montantTtc;
        this.montantAchat = montantAchat;
        this.montantHt = isNull(montantHt) ? 0L : montantHt.longValue();
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        this.montantTaxe = Objects.requireNonNullElse(montantAchat, 0L) - this.montantHt;
    }

    public TaxeDTO(Integer codeTva, Long montantTtc, Long montantAchat, Double montantHt, Long amountToBeTakenIntoAccount) {
        this.codeTva = codeTva;
        this.montantTtc = montantTtc;
        this.montantAchat = montantAchat;
        this.montantHt = isNull(montantHt) ? 0L : montantHt.longValue();
        this.montantTaxe = Objects.requireNonNullElse(montantAchat, 0L) - this.montantHt;
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    }

    public LocalDate getMvtDate() {
        return mvtDate;
    }

    public TaxeDTO setMvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public Long getMontantHt() {
        return montantHt;
    }

    public void setMontantHt(Long montantHt) {
        this.montantHt = montantHt;
    }

    public Long getMontantTaxe() {
        montantTaxe = Objects.requireNonNullElse(montantTtc, 0L) - Objects.requireNonNullElse(this.montantHt, 0L);
        return montantTaxe;
    }

    public void setMontantTaxe(Long montantTaxe) {
        this.montantTaxe = montantTaxe;
    }

    public Long getMontantTtc() {
        return montantTtc;
    }

    public void setMontantTtc(Long montantTtc) {
        this.montantTtc = montantTtc;
    }

    public Long getMontantNet() {
        return montantNet;
    }

    public void setMontantNet(Long montantNet) {
        this.montantNet = montantNet;
    }

    public long getMontantRemise() {
        return montantRemise;
    }

    public void setMontantRemise(long montantRemise) {
        this.montantRemise = montantRemise;
    }

    public Long getMontantAchat() {
        return montantAchat;
    }

    public void setMontantAchat(Long montantAchat) {
        this.montantAchat = montantAchat;
    }

    public Integer getCodeTva() {
        return codeTva;
    }

    public void setCodeTva(Integer codeTva) {
        this.codeTva = codeTva;
    }

    public Long getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public void setAmountToBeTakenIntoAccount(Long amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    }

}
