package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;

import java.time.LocalDate;

public class ProduitAuditingState {

    private LocalDate mvtDate;
    private int initStock;
    private int saleQuantity;
    private int deleveryQuantity;
    private int retourFournisseurQuantity;
    private int perimeQuantity;
    private int ajustementPositifQuantity;
    private int ajustementNegatifQuantity;
    private int deconPositifQuantity;
    private int deconNegatifQuantity;
    private int canceledQuantity;
    private int retourDepot;
    private int storeInventoryQuantity;
    private int inventoryGap;
    private int afterStock;
    private String transactionDate;

    public ProduitAuditingState() {
    }

    public ProduitAuditingState(MouvementProduit mouvementProduitType, LocalDate mvtDate, int initStock, int quantity, int afterStock) {
        this.mvtDate = mvtDate;
        this.initStock = initStock;
        this.saleQuantity = mouvementProduitType == MouvementProduit.SALE ? quantity : 0;
        this.deleveryQuantity = mouvementProduitType == MouvementProduit.COMMANDE ? quantity : 0;
        this.retourFournisseurQuantity = mouvementProduitType == MouvementProduit.RETOUR_FOURNISSEUR ? quantity : 0;
        this.perimeQuantity = mouvementProduitType == MouvementProduit.RETRAIT_PERIME ? quantity : 0;
        this.ajustementPositifQuantity = mouvementProduitType == MouvementProduit.AJUSTEMENT_IN ? quantity : 0;
        this.ajustementNegatifQuantity = mouvementProduitType == MouvementProduit.AJUSTEMENT_OUT ? quantity : 0;
        this.deconPositifQuantity = mouvementProduitType == MouvementProduit.DECONDTION_IN ? quantity : 0;
        this.deconNegatifQuantity = mouvementProduitType == MouvementProduit.DECONDTION_OUT ? quantity : 0;
        this.canceledQuantity = mouvementProduitType == MouvementProduit.CANCEL_SALE  ? quantity : 0;
        this.retourDepot = mouvementProduitType == MouvementProduit.RETOUR_DEPOT ? quantity : 0;
        this.storeInventoryQuantity = mouvementProduitType == MouvementProduit.INVENTAIRE ? quantity : 0;
        this.inventoryGap = mouvementProduitType == MouvementProduit.INVENTAIRE ? initStock - quantity : 0;
        this.afterStock = afterStock;
        // this.transactionDate = transactionDate;
    }
    public ProduitAuditingState(MouvementProduit mouvementProduitType,  int initStock, int quantity, int afterStock) {

        this.initStock = initStock;
        this.saleQuantity = mouvementProduitType == MouvementProduit.SALE ? quantity : 0;
        this.deleveryQuantity = mouvementProduitType == MouvementProduit.COMMANDE ? quantity : 0;
        this.retourFournisseurQuantity = mouvementProduitType == MouvementProduit.RETOUR_FOURNISSEUR ? quantity : 0;
        this.perimeQuantity = mouvementProduitType == MouvementProduit.RETRAIT_PERIME ? quantity : 0;
        this.ajustementPositifQuantity = mouvementProduitType == MouvementProduit.AJUSTEMENT_IN ? quantity : 0;
        this.ajustementNegatifQuantity = mouvementProduitType == MouvementProduit.AJUSTEMENT_OUT ? quantity : 0;
        this.deconPositifQuantity = mouvementProduitType == MouvementProduit.DECONDTION_IN ? quantity : 0;
        this.deconNegatifQuantity = mouvementProduitType == MouvementProduit.DECONDTION_OUT ? quantity : 0;
        this.canceledQuantity = mouvementProduitType == MouvementProduit.CANCEL_SALE  ? quantity : 0;
        this.retourDepot = mouvementProduitType == MouvementProduit.RETOUR_DEPOT ? quantity : 0;
        this.storeInventoryQuantity = mouvementProduitType == MouvementProduit.INVENTAIRE ? quantity : 0;
        this.inventoryGap = mouvementProduitType == MouvementProduit.INVENTAIRE ? initStock - quantity : 0;
        this.afterStock = afterStock;
        // this.transactionDate = transactionDate;
    }
    public String getTransactionDate() {
        return transactionDate;
    }

    public ProduitAuditingState setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
        return this;
    }

    public LocalDate getMvtDate() {
        return mvtDate;
    }

    public ProduitAuditingState setMvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public int getInitStock() {
        return initStock;
    }

    public ProduitAuditingState setInitStock(int initStock) {
        this.initStock = initStock;
        return this;
    }

    public int getSaleQuantity() {
        return saleQuantity;
    }

    public ProduitAuditingState setSaleQuantity(int saleQuantity) {
        this.saleQuantity = saleQuantity;
        return this;
    }

    public int getDeleveryQuantity() {
        return deleveryQuantity;
    }

    public ProduitAuditingState setDeleveryQuantity(int deleveryQuantity) {
        this.deleveryQuantity = deleveryQuantity;
        return this;
    }

    public int getRetourFournisseurQuantity() {
        return retourFournisseurQuantity;
    }

    public ProduitAuditingState setRetourFournisseurQuantity(int retourFournisseurQuantity) {
        this.retourFournisseurQuantity = retourFournisseurQuantity;
        return this;
    }

    public int getPerimeQuantity() {
        return perimeQuantity;
    }

    public ProduitAuditingState setPerimeQuantity(int perimeQuantity) {
        this.perimeQuantity = perimeQuantity;
        return this;
    }

    public int getAjustementPositifQuantity() {
        return ajustementPositifQuantity;
    }

    public ProduitAuditingState setAjustementPositifQuantity(int ajustementPositifQuantity) {
        this.ajustementPositifQuantity = ajustementPositifQuantity;
        return this;
    }

    public int getAjustementNegatifQuantity() {
        return ajustementNegatifQuantity;
    }

    public ProduitAuditingState setAjustementNegatifQuantity(int ajustementNegatifQuantity) {
        this.ajustementNegatifQuantity = ajustementNegatifQuantity;
        return this;
    }

    public int getDeconPositifQuantity() {
        return deconPositifQuantity;
    }

    public ProduitAuditingState setDeconPositifQuantity(int deconPositifQuantity) {
        this.deconPositifQuantity = deconPositifQuantity;
        return this;
    }

    public int getDeconNegatifQuantity() {
        return deconNegatifQuantity;
    }

    public ProduitAuditingState setDeconNegatifQuantity(int deconNegatifQuantity) {
        this.deconNegatifQuantity = deconNegatifQuantity;
        return this;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }

    public ProduitAuditingState setCanceledQuantity(int canceledQuantity) {
        this.canceledQuantity = canceledQuantity;
        return this;
    }

    public int getRetourDepot() {
        return retourDepot;
    }

    public ProduitAuditingState setRetourDepot(int retourDepot) {
        this.retourDepot = retourDepot;
        return this;
    }

    public int getStoreInventoryQuantity() {
        return storeInventoryQuantity;
    }

    public ProduitAuditingState setStoreInventoryQuantity(int storeInventoryQuantity) {
        this.storeInventoryQuantity = storeInventoryQuantity;
        return this;
    }

    public int getInventoryGap() {
        return inventoryGap;
    }

    public ProduitAuditingState setInventoryGap(int inventoryGap) {
        this.inventoryGap = inventoryGap;
        return this;
    }

    public int getAfterStock() {
        return afterStock;
    }

    public ProduitAuditingState setAfterStock(int afterStock) {
        this.afterStock = afterStock;
        return this;
    }


}
