package com.kobe.warehouse.service.dto;


import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StockProduit;

import java.time.Instant;

public class StockProduitDTO {

    private Long id;
    private Integer qtyStock;
    private int qtyVirtual;
    private int qtyUG;
    private Long rayonId;
    private String rayonLibelle;
    private String rayonCode;
    private Long produitId;
    private Instant createdAt;
    private Instant updatedAt;
    private String produitLibelle;

    public Long getId() {
        return id;
    }

    public StockProduitDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getQtyStock() {
        return qtyStock;
    }

    public StockProduitDTO setQtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
        return this;
    }

    public int getQtyVirtual() {
        return qtyVirtual;
    }

    public StockProduitDTO setQtyVirtual(int qtyVirtual) {
        this.qtyVirtual = qtyVirtual;
        return this;
    }

    public int getQtyUG() {
        return qtyUG;
    }

    public StockProduitDTO setQtyUG(int qtyUG) {
        this.qtyUG = qtyUG;
        return this;
    }

    public Long getRayonId() {
        return rayonId;
    }

    public StockProduitDTO setRayonId(Long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public String getRayonLibelle() {
        return rayonLibelle;
    }

    public StockProduitDTO setRayonLibelle(String rayonLibelle) {
        this.rayonLibelle = rayonLibelle;
        return this;
    }

    public String getRayonCode() {
        return rayonCode;
    }

    public StockProduitDTO setRayonCode(String rayonCode) {
        this.rayonCode = rayonCode;
        return this;
    }


    public Long getProduitId() {
        return produitId;
    }

    public StockProduitDTO setProduitId(Long produitId) {
        this.produitId = produitId;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public StockProduitDTO setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public StockProduitDTO setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public StockProduitDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public StockProduitDTO(StockProduit s) {
        this.id = s.getId();
        this.qtyStock = s.getQtyStock();
        this.qtyVirtual = s.getQtyVirtual();
        this.qtyUG = s.getQtyUG();
        Rayon r = s.getRayon();
        this.rayonId = r.getId();
        this.rayonLibelle = r.getLibelle();
        this.rayonCode = r.getCode();
        Produit p = s.getProduit();
        this.produitId = p.getId();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();
        this.produitLibelle = p.getLibelle();
    }

    public StockProduitDTO() {
    }
}
