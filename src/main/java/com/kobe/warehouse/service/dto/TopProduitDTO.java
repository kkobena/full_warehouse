package com.kobe.warehouse.service.dto;

import java.math.BigDecimal;

public class TopProduitDTO {
    private Long id;
    private String libelle;
    private String codeCip;
    private Long quantiteTotale;
    private BigDecimal cumul;
    private BigDecimal totalGlobal;
    private Integer salesAmount;

    public TopProduitDTO() {}

    public TopProduitDTO(Long id, String libelle, String codeCip, Long quantiteTotale, BigDecimal cumul, BigDecimal totalGlobal, Integer salesAmount) {
        this.id = id;
        this.libelle = libelle;
        this.codeCip = codeCip;
        this.quantiteTotale = quantiteTotale;
        this.cumul = cumul;
        this.totalGlobal = totalGlobal;
        this.salesAmount = salesAmount;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public void setCodeCip(String codeCip) {
        this.codeCip = codeCip;
    }

    public Long getQuantiteTotale() {
        return quantiteTotale;
    }

    public void setQuantiteTotale(Long quantiteTotale) {
        this.quantiteTotale = quantiteTotale;
    }

    public BigDecimal getCumul() {
        return cumul;
    }

    public void setCumul(BigDecimal cumul) {
        this.cumul = cumul;
    }

    public BigDecimal getTotalGlobal() {
        return totalGlobal;
    }

    public void setTotalGlobal(BigDecimal totalGlobal) {
        this.totalGlobal = totalGlobal;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }
}
