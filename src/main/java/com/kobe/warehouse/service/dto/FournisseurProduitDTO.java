package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;

public class FournisseurProduitDTO {

    private Long id;
    private String codeCip;
    @NotNull
    @Min(value = 1)
    private Integer prixAchat;
    @NotNull
    @Min(value = 1)
    private Integer prixUni;
    private Instant createdAt;
    private Instant updatedAt;
    private Long produitId;
    private String produitLibelle;
    private Long fournisseurId;
    private String fournisseurLibelle;
    private boolean principal;

    public Long getId() {
        return id;
    }

    public FournisseurProduitDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public FournisseurProduitDTO setCodeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public FournisseurProduitDTO setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public Integer getPrixUni() {
        return prixUni;
    }

    public FournisseurProduitDTO setPrixUni(Integer prixUni) {
        this.prixUni = prixUni;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public FournisseurProduitDTO setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public FournisseurProduitDTO setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Long getProduitId() {
        return produitId;
    }

    public FournisseurProduitDTO setProduitId(Long produitId) {
        this.produitId = produitId;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public FournisseurProduitDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public FournisseurProduitDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }

    public FournisseurProduitDTO setFournisseurLibelle(String fournisseurLibelle) {
        this.fournisseurLibelle = fournisseurLibelle;
        return this;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public FournisseurProduitDTO setPrincipal(boolean principal) {
        this.principal = principal;
        return this;
    }

    public FournisseurProduitDTO() {
    }

    public FournisseurProduitDTO(FournisseurProduit f) {
        this.id = f.getId();
        this.codeCip = f.getCodeCip();
        this.prixAchat = f.getPrixAchat();
        this.prixUni = f.getPrixUni();
        this.createdAt = f.getCreatedAt();
        this.updatedAt = f.getUpdatedAt();
        Produit p=f.getProduit();
        this.produitId = p.getId();
        this.produitLibelle = p.getLibelle();
        Fournisseur fr=f.getFournisseur();
        this.fournisseurId = fr.getId();
        this.fournisseurLibelle = fr.getLibelle();
        this.principal = f.isPrincipal();
    }
}
