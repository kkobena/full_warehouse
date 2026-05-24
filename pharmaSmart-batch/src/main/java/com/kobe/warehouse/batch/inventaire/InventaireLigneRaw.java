package com.kobe.warehouse.batch.inventaire;

/**
 * Représentation brute d'une ligne CSV d'inventaire avant validation.
 */
public class InventaireLigneRaw {

    private String cip13;
    private String lot;
    private String quantite;
    private String peremption;

    public InventaireLigneRaw() {}

    public String getCip13() { return cip13; }
    public void setCip13(String cip13) { this.cip13 = cip13; }

    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }

    public String getQuantite() { return quantite; }
    public void setQuantite(String quantite) { this.quantite = quantite; }

    public String getPeremption() { return peremption; }
    public void setPeremption(String peremption) { this.peremption = peremption; }
}
