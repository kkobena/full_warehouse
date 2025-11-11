package com.kobe.warehouse.service.stock.dto;

import java.time.LocalDate;

public class LotFilterParam {

    private  Integer dayCount;
    private Integer produitId;
    private String numLot;
    private String searchTerm;
    private LocalDate fromDate = LocalDate.now().plusMonths(1);
    private LocalDate toDate;
    private Integer fournisseurId;
    private Integer rayonId;
    private Integer familleProduitId;
    private Integer magasinId;
    private Integer storageId;
    private TypeFilter type;

    public Integer getDayCount() {
        return dayCount;
    }

    public LotFilterParam setDayCount(Integer dayCount) {
        this.dayCount = dayCount;
        return this;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public void setStorageId(Integer storageId) {
        this.storageId = storageId;
    }

    public Integer getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Integer fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public Integer getFamilleProduitId() {
        return familleProduitId;
    }

    public void setFamilleProduitId(Integer familleProduitId) {
        this.familleProduitId = familleProduitId;
    }

    public TypeFilter getType() {
        return type;
    }

    public void setType(TypeFilter type) {
        this.type = type;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public String getNumLot() {
        return numLot;
    }

    public void setNumLot(String numLot) {
        this.numLot = numLot;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public Integer getRayonId() {
        return rayonId;
    }

    public void setRayonId(Integer rayonId) {
        this.rayonId = rayonId;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    public void setMagasinId(Integer magasinId) {
        this.magasinId = magasinId;
    }
}
