package com.kobe.warehouse.service.financiel_transaction.dto;

import java.time.LocalDate;

public class AchatDTO {

    private long montantNet;
    private long montantTtc;
    private long montantHt;
    private long montantTaxe;
    private Integer groupeGrossisteId;
    private String groupeGrossiste;
    private long montantRemise;
    private int ordreAffichage;
    private LocalDate mvtDate;

    public long getMontantNet() {
        return montantNet;
    }

    public AchatDTO setMontantNet(long montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public LocalDate getMvtDate() {
        return mvtDate;
    }

    public AchatDTO setMvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public int getOrdreAffichage() {
        return ordreAffichage;
    }

    public AchatDTO setOrdreAffichage(int ordreAffichage) {
        this.ordreAffichage = ordreAffichage;
        return this;
    }

    public long getMontantRemise() {
        return montantRemise;
    }

    public AchatDTO setMontantRemise(long montantRemise) {
        this.montantRemise = montantRemise;
        return this;
    }

    public long getMontantTtc() {
        return montantTtc;
    }

    public AchatDTO setMontantTtc(long montantTtc) {
        this.montantTtc = montantTtc;
        return this;
    }

    public long getMontantHt() {
        return montantHt;
    }

    public AchatDTO setMontantHt(long montantHt) {
        this.montantHt = montantHt;
        return this;
    }

    public long getMontantTaxe() {
        return montantTaxe;
    }

    public AchatDTO setMontantTaxe(long montantTaxe) {
        this.montantTaxe = montantTaxe;
        return this;
    }

    public Integer getGroupeGrossisteId() {
        return groupeGrossisteId;
    }

    public AchatDTO setGroupeGrossisteId(Integer groupeGrossisteId) {
        this.groupeGrossisteId = groupeGrossisteId;
        return this;
    }

    public String getGroupeGrossiste() {
        return groupeGrossiste;
    }

    public AchatDTO setGroupeGrossiste(String groupeGrossiste) {
        this.groupeGrossiste = groupeGrossiste;
        return this;
    }

    public AchatDTO() {}

    public AchatDTO(
        LocalDate mvtDate,
        Long montantNet,
        Long montantTaxe,
        Long montantTtc,
        Long montantRemise,
        Integer groupeGrossisteId,
        String groupeGrossiste,
        Integer ordreAffichage
    ) {
        this.mvtDate = mvtDate;
        this.montantNet = montantNet;
        this.montantTaxe = montantTaxe;
        this.montantTtc = montantTtc;
        this.montantRemise = montantRemise;
        this.groupeGrossisteId = groupeGrossisteId;
        this.groupeGrossiste = groupeGrossiste;
        this.ordreAffichage = ordreAffichage;
    }
}
