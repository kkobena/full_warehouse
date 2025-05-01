package com.kobe.warehouse.domain;

public class PrixRererence {

    private int prix;
    private long tiersPayantId;

    public int prix() {
        return prix;
    }

    public void setPrix(int prix) {
        this.prix = prix;
    }

    public long tiersPayantId() {
        return tiersPayantId;
    }

    public void setTiersPayantId(long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
    }
}
