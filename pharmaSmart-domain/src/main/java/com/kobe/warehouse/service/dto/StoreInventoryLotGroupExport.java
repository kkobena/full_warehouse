package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.List;

public class StoreInventoryLotGroupExport {

    private final String codeCip;
    private final String produitLibelle;
    private final List<StoreInventoryLotLineExport> lots = new ArrayList<>();
    private long totalInit;
    private long totalOnHand;
    private long totalGap;
    private long valeurAvantAchat;
    private long valeurApresAchat;
    private long valeurAvantVente;
    private long valeurApresVente;

    public StoreInventoryLotGroupExport(String codeCip, String produitLibelle) {
        this.codeCip = codeCip;
        this.produitLibelle = produitLibelle;
    }

    public void addLot(StoreInventoryLotLineExport lot) {
        lots.add(lot);
        totalInit    += lot.getQuantityInit();
        totalOnHand  += lot.getQuantityOnHand();
        totalGap     += lot.getGap();
        valeurAvantAchat += (long) lot.getQuantityInit()   * lot.getPrixAchat();
        valeurApresAchat += (long) lot.getQuantityOnHand() * lot.getPrixAchat();
        valeurAvantVente += (long) lot.getQuantityInit()   * lot.getLastUnitPrice();
        valeurApresVente += (long) lot.getQuantityOnHand() * lot.getLastUnitPrice();
    }

    public String getCodeCip()             { return codeCip; }
    public String getProduitLibelle()      { return produitLibelle; }
    public List<StoreInventoryLotLineExport> getLots() { return lots; }
    public long getTotalInit()             { return totalInit; }
    public long getTotalOnHand()           { return totalOnHand; }
    public long getTotalGap()              { return totalGap; }
    public long getValeurAvantAchat()      { return valeurAvantAchat; }
    public long getValeurApresAchat()      { return valeurApresAchat; }
    public long getValeurAvantVente()      { return valeurAvantVente; }
    public long getValeurApresVente()      { return valeurApresVente; }
    public long getEcartAchat()            { return valeurApresAchat - valeurAvantAchat; }
    public long getEcartVente()            { return valeurApresVente - valeurAvantVente; }
}
