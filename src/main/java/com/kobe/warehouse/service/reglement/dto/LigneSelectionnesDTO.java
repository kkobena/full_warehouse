package com.kobe.warehouse.service.reglement.dto;

public class LigneSelectionnesDTO {

    private int id;
    private int montantVerse;
    private int montantAttendu;

    public int getId() {
        return id;
    }

    public LigneSelectionnesDTO setId(int id) {
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
}
