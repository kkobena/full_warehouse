package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.FactureItemId;

public class LigneSelectionnesDTO {

    private FactureItemId id;
    private int montantVerse;
    private int montantAttendu;
    private int montantFacture;

    public FactureItemId getId() {
        return id;
    }

    public LigneSelectionnesDTO setId(FactureItemId id) {
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
