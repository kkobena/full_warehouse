package com.kobe.warehouse.domain;

public class TiersPayantPrix {
    long tiersPayantId;
    long refrenceId;
    int prix;
    int montant;

    public long getTiersPayantId() {
        return tiersPayantId;
    }

    public TiersPayantPrix setTiersPayantId(long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

    public long getRefrenceId() {
        return refrenceId;
    }

    public TiersPayantPrix setRefrenceId(long refrenceId) {
        this.refrenceId = refrenceId;
        return this;
    }

    public int getPrix() {
        return prix;
    }

    public TiersPayantPrix setPrix(int prix) {
        this.prix = prix;
        return this;
    }

    public int getMontant() {
        return montant;
    }

    public TiersPayantPrix setMontant(int montant) {
        this.montant = montant;
        return this;
    }
}
