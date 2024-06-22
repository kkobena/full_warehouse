package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FournisseurProduitDTO {

  private Long id;
  private String codeCip;

  @NotNull
  @Min(value = 1)
  private Integer prixAchat;

  @NotNull
  @Min(value = 1)
  private Integer prixUni;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long produitId;
  private String produitLibelle;
  private Long fournisseurId;
  private String fournisseurLibelle;
  private boolean principal;
  private ProduitDTO produit;

  public FournisseurProduitDTO() {}

  public FournisseurProduitDTO(FournisseurProduit f) {
    id = f.getId();
    codeCip = f.getCodeCip();
    prixAchat = f.getPrixAchat();
    prixUni = f.getPrixUni();
    createdAt = f.getCreatedAt();
    updatedAt = f.getUpdatedAt();
    Produit p = f.getProduit();
    produitId = p.getId();
    produitLibelle = p.getLibelle();
    Fournisseur fr = f.getFournisseur();
    fournisseurId = fr.getId();
    fournisseurLibelle = fr.getLibelle();
    principal = f.isPrincipal();
  }

  public static FournisseurProduitDTO fromEntity(FournisseurProduit f) {
    Produit p = f.getProduit();

    Fournisseur fr = f.getFournisseur();
    return new FournisseurProduitDTO()
        .setId(f.getId())
        .setCodeCip(f.getCodeCip())
        .setPrixAchat(f.getPrixAchat())
        .setPrixUni(f.getPrixUni())
        .setFournisseurLibelle(fr.getLibelle())
        .setProduitLibelle(p.getLibelle())
        .setProduitId(p.getId())
        .setFournisseurId(fr.getId())
        .setCreatedAt(f.getCreatedAt())
        .setUpdatedAt(f.getUpdatedAt())
        .setProduit(ProduitBuilder.fromEntity(p))
        .setPrincipal(f.isPrincipal());
  }

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

  public @NotNull @Min(value = 1) Integer getPrixAchat() {
    return prixAchat;
  }

  public FournisseurProduitDTO setPrixAchat(Integer prixAchat) {
    this.prixAchat = prixAchat;
    return this;
  }

  public @NotNull @Min(value = 1) Integer getPrixUni() {
    return prixUni;
  }

  public FournisseurProduitDTO setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public FournisseurProduitDTO setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public FournisseurProduitDTO setUpdatedAt(LocalDateTime updatedAt) {
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

  public ProduitDTO getProduit() {
    return produit;
  }

  public FournisseurProduitDTO setProduit(ProduitDTO produit) {
    this.produit = produit;

    return this;
  }
}
