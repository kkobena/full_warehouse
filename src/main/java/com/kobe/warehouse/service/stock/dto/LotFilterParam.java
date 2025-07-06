package com.kobe.warehouse.service.stock.dto;

import java.time.LocalDate;

public class LotFilterParam {

    int dayCount;
    private Long produitId;
    private String numLot;
    private String searchTerm;
    private LocalDate fromDate = LocalDate.now().plusMonths(1);
    private LocalDate toDate;
    private Long fournisseurId;
    private Long rayonId;
    private Long familleProduitId;

    public int getDayCount() {
        return dayCount;
    }

    public LotFilterParam setDayCount(int dayCount) {
        this.dayCount = dayCount;
        return this;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public Long getFamilleProduitId() {
        return familleProduitId;
    }

    public void setFamilleProduitId(Long familleProduitId) {
        this.familleProduitId = familleProduitId;
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

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public Long getRayonId() {
        return rayonId;
    }

    public void setRayonId(Long rayonId) {
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
}
