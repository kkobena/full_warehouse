package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

public class StoreInventoryLotLineExport {

    private final String codeCip;
    private final String produitLibelle;
    private final String numLot;
    private final LocalDate expiryDate;
    private final int quantityInit;
    private final int quantityOnHand;
    private final int gap;
    private final int lastUnitPrice;
    private final int prixAchat;

    public StoreInventoryLotLineExport(
        String codeCip, String produitLibelle, String numLot, LocalDate expiryDate,
        int quantityInit, int quantityOnHand, int gap, int lastUnitPrice, int prixAchat
    ) {
        this.codeCip = codeCip;
        this.produitLibelle = produitLibelle;
        this.numLot = numLot;
        this.expiryDate = expiryDate;
        this.quantityInit = quantityInit;
        this.quantityOnHand = quantityOnHand;
        this.gap = gap;
        this.lastUnitPrice = lastUnitPrice;
        this.prixAchat = prixAchat;
    }

    public String getCodeCip()           { return codeCip; }
    public String getProduitLibelle()    { return produitLibelle; }
    public String getNumLot()            { return numLot; }
    public LocalDate getExpiryDate()     { return expiryDate; }
    public int getQuantityInit()         { return quantityInit; }
    public int getQuantityOnHand()       { return quantityOnHand; }
    public int getGap()                  { return gap; }
    public int getLastUnitPrice()        { return lastUnitPrice; }
    public int getPrixAchat()            { return prixAchat; }
}
