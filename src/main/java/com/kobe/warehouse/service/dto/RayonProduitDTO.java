package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.Storage;

public class RayonProduitDTO {

    private String codeRayon;
    private String libelleRayon;
    private String libelleStorage;
    private String storageType;
    private String magasin;
    private Long magasinId;
    private Long rayonId;
    private Long id;
    private Long produitId;

    public RayonProduitDTO(RayonProduit produit) {
        Rayon rayon = produit.getRayon();
        Storage storage = rayon.getStorage();
        Magasin thatmagasin = storage.getMagasin();
        this.codeRayon = rayon.getCode();
        this.libelleRayon = rayon.getLibelle();
        this.libelleStorage = storage.getName();
        this.storageType = storage.getStorageType().value;
        this.magasin = thatmagasin.getName();
        this.magasinId = thatmagasin.getId();
        this.rayonId = rayon.getId();
        this.id = produit.getId();
        this.produitId = produit.getProduit().getId();
    }

    public Long getProduitId() {
        return produitId;
    }

    public RayonProduitDTO setProduitId(Long produitId) {
        this.produitId = produitId;
        return this;
    }

    public Long getRayonId() {
        return rayonId;
    }

    public RayonProduitDTO setRayonId(Long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public String getMagasin() {
        return magasin;
    }

    public RayonProduitDTO setMagasin(String magasin) {
        this.magasin = magasin;
        return this;
    }

    public Long getMagasinId() {
        return magasinId;
    }

    public RayonProduitDTO setMagasinId(Long magasinId) {
        this.magasinId = magasinId;
        return this;
    }

    public String getStorageType() {
        return storageType;
    }

    public RayonProduitDTO setStorageType(String storageType) {
        this.storageType = storageType;
        return this;
    }

    public String getCodeRayon() {
        return codeRayon;
    }

    public RayonProduitDTO setCodeRayon(String codeRayon) {
        this.codeRayon = codeRayon;
        return this;
    }

    public String getLibelleRayon() {
        return libelleRayon;
    }

    public RayonProduitDTO setLibelleRayon(String libelleRayon) {
        this.libelleRayon = libelleRayon;
        return this;
    }

    public String getLibelleStorage() {
        return libelleStorage;
    }

    public RayonProduitDTO setLibelleStorage(String libelleStorage) {
        this.libelleStorage = libelleStorage;
        return this;
    }

    public Long getId() {
        return id;
    }

    public RayonProduitDTO setId(Long id) {
        this.id = id;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RayonProduitDTO{");
        sb.append("codeRayon='").append(codeRayon).append('\'');
        sb.append(", libelleRayon='").append(libelleRayon).append('\'');
        sb.append(", libelleStorage='").append(libelleStorage).append('\'');
        sb.append(", storageType=").append(storageType);
        sb.append(", magasin='").append(magasin).append('\'');
        sb.append(", magasinId=").append(magasinId);
        sb.append(", rayonId=").append(rayonId);
        sb.append('}');
        return sb.toString();
    }
}
