package com.kobe.warehouse.service.reglement.dto;

public class LigneSelectionnesDTO {

    private long id;
    private int montantVerse;
    private int montantAttendu;
    private int montantFacture;

    public long getId() {
        return id;
    }

    public LigneSelectionnesDTO setId(long id) {
        this.id = id;
        return this;
    }

    public int getMontantAttendu() {
        return montantAttendu;
    }

    public LigneSelectionnesDTO setMontantAttendu(int montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public int getMontantVerse() {
        return montantVerse;
    }

    public LigneSelectionnesDTO setMontantVerse(int montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public int getMontantFacture() {
        return montantFacture;
    }

    public LigneSelectionnesDTO setMontantFacture(int montantFacture) {
        this.montantFacture = montantFacture;
        return this;
    }
}
