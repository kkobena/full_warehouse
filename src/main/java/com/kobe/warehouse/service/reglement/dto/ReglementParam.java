package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReglementParam {

    private long id;

    @NotNull
    private ModeEditionReglement mode;

    private LocalDate paymentDate = LocalDate.now();
    private boolean partialPayment;
    private ModePaimentCode modePaimentCode;
    private BanqueInfoDTO banqueInfo;
    private int amount;
    private String comment;
    private List<Long> dossierIds = new ArrayList<>();
    private List<LigneSelectionnesDTO> ligneSelectionnes = new ArrayList<>();
    private int totalAmount;
    private int montantFacture;

    public int getMontantFacture() {
        return montantFacture;
    }

    public ReglementParam setMontantFacture(int montantFacture) {
        this.montantFacture = montantFacture;
        return this;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public ReglementParam setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public long getId() {
        return id;
    }

    public ReglementParam setId(long id) {
        this.id = id;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public ReglementParam setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public List<LigneSelectionnesDTO> getLigneSelectionnes() {
        return ligneSelectionnes;
    }

    public ReglementParam setLigneSelectionnes(List<LigneSelectionnesDTO> ligneSelectionnes) {
        this.ligneSelectionnes = ligneSelectionnes;
        return this;
    }

    public BanqueInfoDTO getBanqueInfo() {
        return banqueInfo;
    }

    public ReglementParam setBanqueInfo(BanqueInfoDTO banqueInfo) {
        this.banqueInfo = banqueInfo;
        return this;
    }

    public List<Long> getDossierIds() {
        return dossierIds;
    }

    public ReglementParam setDossierIds(List<Long> dossierIds) {
        this.dossierIds = dossierIds;
        return this;
    }

    public ModeEditionReglement getMode() {
        return mode;
    }

    public ReglementParam setMode(ModeEditionReglement mode) {
        this.mode = mode;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ReglementParam setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ModePaimentCode getModePaimentCode() {
        return modePaimentCode;
    }

    public ReglementParam setModePaimentCode(ModePaimentCode modePaimentCode) {
        this.modePaimentCode = modePaimentCode;
        return this;
    }

    public boolean isPartialPayment() {
        return partialPayment;
    }

    public ReglementParam setPartialPayment(boolean partialPayment) {
        this.partialPayment = partialPayment;
        return this;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public ReglementParam setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
        return this;
    }
}
