package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;

import java.time.LocalDate;

public class ProduitAuditingState {

    private LocalDate mvtDate;
    private Integer initStock;
    private Integer saleQuantity;
    private Integer deleveryQuantity;
    private Integer retourFournisseurQuantity;
    private Integer perimeQuantity;
    private Integer ajustementPositifQuantity;
    private Integer ajustementNegatifQuantity;
    private Integer deconPositifQuantity;
    private Integer deconNegatifQuantity;
    private Integer canceledQuantity;
    private Integer retourDepot;
    private Integer storeInventoryQuantity;
    private Integer inventoryGap;
    private Integer afterStock;
    private String transactionDate;

    public ProduitAuditingState() {
    }

    public ProduitAuditingState(MouvementProduit mouvementProduitType, LocalDate mvtDate, Integer initStock, Integer quantity, Integer afterStock) {
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
    public ProduitAuditingState(MouvementProduit mouvementProduitType,  Integer initStock, Integer quantity, Integer afterStock) {

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

    public Integer getInitStock() {
        return initStock;
    }

    public ProduitAuditingState setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public Integer getSaleQuantity() {
        return saleQuantity;
    }

    public ProduitAuditingState setSaleQuantity(Integer saleQuantity) {
        this.saleQuantity = saleQuantity;
        return this;
    }

    public Integer getDeleveryQuantity() {
        return deleveryQuantity;
    }

    public ProduitAuditingState setDeleveryQuantity(Integer deleveryQuantity) {
        this.deleveryQuantity = deleveryQuantity;
        return this;
    }

    public Integer getRetourFournisseurQuantity() {
        return retourFournisseurQuantity;
    }

    public ProduitAuditingState setRetourFournisseurQuantity(Integer retourFournisseurQuantity) {
        this.retourFournisseurQuantity = retourFournisseurQuantity;
        return this;
    }

    public Integer getPerimeQuantity() {
        return perimeQuantity;
    }

    public ProduitAuditingState setPerimeQuantity(Integer perimeQuantity) {
        this.perimeQuantity = perimeQuantity;
        return this;
    }

    public Integer getAjustementPositifQuantity() {
        return ajustementPositifQuantity;
    }

    public ProduitAuditingState setAjustementPositifQuantity(Integer ajustementPositifQuantity) {
        this.ajustementPositifQuantity = ajustementPositifQuantity;
        return this;
    }

    public Integer getAjustementNegatifQuantity() {
        return ajustementNegatifQuantity;
    }

    public ProduitAuditingState setAjustementNegatifQuantity(Integer ajustementNegatifQuantity) {
        this.ajustementNegatifQuantity = ajustementNegatifQuantity;
        return this;
    }

    public Integer getDeconPositifQuantity() {
        return deconPositifQuantity;
    }

    public ProduitAuditingState setDeconPositifQuantity(Integer deconPositifQuantity) {
        this.deconPositifQuantity = deconPositifQuantity;
        return this;
    }

    public Integer getDeconNegatifQuantity() {
        return deconNegatifQuantity;
    }

    public ProduitAuditingState setDeconNegatifQuantity(Integer deconNegatifQuantity) {
        this.deconNegatifQuantity = deconNegatifQuantity;
        return this;
    }

    public Integer getCanceledQuantity() {
        return canceledQuantity;
    }

    public ProduitAuditingState setCanceledQuantity(Integer canceledQuantity) {
        this.canceledQuantity = canceledQuantity;
        return this;
    }

    public Integer getRetourDepot() {
        return retourDepot;
    }

    public ProduitAuditingState setRetourDepot(Integer retourDepot) {
        this.retourDepot = retourDepot;
        return this;
    }

    public Integer getStoreInventoryQuantity() {
        return storeInventoryQuantity;
    }

    public ProduitAuditingState setStoreInventoryQuantity(Integer storeInventoryQuantity) {
        this.storeInventoryQuantity = storeInventoryQuantity;
        return this;
    }

    public Integer getInventoryGap() {
        return inventoryGap;
    }

    public ProduitAuditingState setInventoryGap(Integer inventoryGap) {
        this.inventoryGap = inventoryGap;
        return this;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public ProduitAuditingState setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
        return this;
    }


}
