package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.*;

public class RepartitionStockProduitDTO {

    private int qtyMvt;
    private long stockProduitSourceId;
    private long storageDestinationId;
    private int destFinalStock;
    private int destInitStock;
    private int sourceFinalStock;
    private int sourceInitStock;
    private long produitId;
    private String produitLibelle;
    private String produitCip;
    private String storageSourceLibelle;
    private String storageDestLibelle;
    private long storageDestId;
    private long storageSourceId;
    private String user;

    public int getQtyMvt() {
        return qtyMvt;
    }

    public String getStorageSourceLibelle() {
        return storageSourceLibelle;
    }

    public RepartitionStockProduitDTO setStorageSourceLibelle(String storageSourceLibelle) {
        this.storageSourceLibelle = storageSourceLibelle;
        return this;
    }

    public String getStorageDestLibelle() {
        return storageDestLibelle;
    }

    public RepartitionStockProduitDTO setStorageDestLibelle(String storageDestLibelle) {
        this.storageDestLibelle = storageDestLibelle;
        return this;
    }

    public long getStorageDestId() {
        return storageDestId;
    }

    public RepartitionStockProduitDTO setStorageDestId(long storageDestId) {
        this.storageDestId = storageDestId;
        return this;
    }

    public long getStorageSourceId() {
        return storageSourceId;
    }

    public RepartitionStockProduitDTO setStorageSourceId(long storageSourceId) {
        this.storageSourceId = storageSourceId;
        return this;
    }

    public RepartitionStockProduitDTO setQtyMvt(int qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public long getStockProduitSourceId() {
        return stockProduitSourceId;
    }

    public RepartitionStockProduitDTO setStockProduitSourceId(long stockProduitSourceId) {
        this.stockProduitSourceId = stockProduitSourceId;
        return this;
    }

    public long getStorageDestinationId() {
        return storageDestinationId;
    }

    public RepartitionStockProduitDTO setStorageDestinationId(long storageDestinationId) {
        this.storageDestinationId = storageDestinationId;
        return this;
    }

    public int getDestFinalStock() {
        return destFinalStock;
    }

    public RepartitionStockProduitDTO setDestFinalStock(int destFinalStock) {
        this.destFinalStock = destFinalStock;
        return this;
    }

    public int getDestInitStock() {
        return destInitStock;
    }

    public RepartitionStockProduitDTO setDestInitStock(int destInitStock) {
        this.destInitStock = destInitStock;
        return this;
    }

    public int getSourceFinalStock() {
        return sourceFinalStock;
    }

    public RepartitionStockProduitDTO setSourceFinalStock(int sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
        return this;
    }

    public int getSourceInitStock() {
        return sourceInitStock;
    }

    public RepartitionStockProduitDTO setSourceInitStock(int sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
        return this;
    }

    public long getProduitId() {
        return produitId;
    }

    public RepartitionStockProduitDTO setProduitId(long produitId) {
        this.produitId = produitId;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public RepartitionStockProduitDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public RepartitionStockProduitDTO setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public String getUser() {
        return user;
    }

    public RepartitionStockProduitDTO setUser(String user) {
        this.user = user;
        return this;
    }

    public RepartitionStockProduitDTO() {}

    public RepartitionStockProduitDTO(RepartitionStockProduit repartitionStockProduit) {
        this.qtyMvt = repartitionStockProduit.getQtyMvt();
        StockProduit source = repartitionStockProduit.getStockProduitSource();
        StockProduit dest = repartitionStockProduit.getStockProduitDestination();
        this.stockProduitSourceId = source.getId();
        this.storageDestinationId = dest.getStorage().getId();
        this.destFinalStock = repartitionStockProduit.getDestFinalStock();
        this.destInitStock = repartitionStockProduit.getDestInitStock();
        this.sourceFinalStock = repartitionStockProduit.getSourceFinalStock();
        this.sourceInitStock = repartitionStockProduit.getSourceInitStock();
        Produit produit = source.getProduit();
        this.produitId = produit.getId();
        this.produitLibelle = produit.getLibelle();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduit != null) {
            this.produitCip = fournisseurProduit.getCodeCip();
        }
        Storage storageSource = source.getStorage();
        Storage storageDest = dest.getStorage();
        this.storageSourceLibelle = storageSource.getName();
        this.storageSourceId = storageSource.getId();
        this.storageDestId = storageDest.getId();
        this.storageDestLibelle = storageDest.getName();
        AppUser oUser = repartitionStockProduit.getUser();
        this.user = String.format("%s %s", oUser.getFirstName(), oUser.getLastName());
    }
}
