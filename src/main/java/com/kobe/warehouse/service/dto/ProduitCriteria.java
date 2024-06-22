package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import java.io.Serializable;

public class ProduitCriteria implements Serializable {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String codeEan;
  private String codeCip;
  private String libelle;
  private Status status;
  private Boolean dateperemption;
  private Boolean deconditionnable;
  private Long qtySeuilMini;
  private Integer qtyAppro;
  private Long parentId;
  private Integer prixPaf;
  private Integer prixUni;
  private Long formeId;
  private Long familleId;
  private Long gammeId;
  private Long fabriquantId;
  private Long laboratoireId;
  private Long tvaId;
  private Long magasinId;
  private Long rayonId;
  private String search;
  private Boolean deconditionne;
  private Long remiseId;
  private Long tableauId;
  private Long storageId;
  private TypeProduit typeProduit;
  private Long tableauNot;
  private Long rayonNot;
  private Long remiseNot;

  public Long getId() {
    return id;
  }

  public ProduitCriteria setId(Long id) {
    this.id = id;
    return this;
  }

  public String getCodeEan() {
    return codeEan;
  }

  public ProduitCriteria setCodeEan(String codeEan) {
    this.codeEan = codeEan;
    return this;
  }

  public String getCodeCip() {
    return codeCip;
  }

  public ProduitCriteria setCodeCip(String codeCip) {
    this.codeCip = codeCip;
    return this;
  }

  public String getLibelle() {
    return libelle;
  }

  public ProduitCriteria setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public ProduitCriteria setStatus(Status status) {
    this.status = status;
    return this;
  }

  public Boolean getDateperemption() {
    return dateperemption;
  }

  public ProduitCriteria setDateperemption(Boolean dateperemption) {
    this.dateperemption = dateperemption;
    return this;
  }

  public Boolean getDeconditionnable() {
    return deconditionnable;
  }

  public ProduitCriteria setDeconditionnable(Boolean deconditionnable) {
    this.deconditionnable = deconditionnable;
    return this;
  }

  public Long getQtySeuilMini() {
    return qtySeuilMini;
  }

  public ProduitCriteria setQtySeuilMini(Long qtySeuilMini) {
    this.qtySeuilMini = qtySeuilMini;
    return this;
  }

  public Integer getQtyAppro() {
    return qtyAppro;
  }

  public ProduitCriteria setQtyAppro(Integer qtyAppro) {
    this.qtyAppro = qtyAppro;
    return this;
  }

  public Long getParentId() {
    return parentId;
  }

  public ProduitCriteria setParentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

  public Integer getPrixPaf() {
    return prixPaf;
  }

  public ProduitCriteria setPrixPaf(Integer prixPaf) {
    this.prixPaf = prixPaf;
    return this;
  }

  public Integer getPrixUni() {
    return prixUni;
  }

  public ProduitCriteria setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
    return this;
  }

  public Long getFormeId() {
    return formeId;
  }

  public ProduitCriteria setFormeId(Long formeId) {
    this.formeId = formeId;
    return this;
  }

  public Long getFamilleId() {
    return familleId;
  }

  public ProduitCriteria setFamilleId(Long familleId) {
    this.familleId = familleId;
    return this;
  }

  public Long getGammeId() {
    return gammeId;
  }

  public ProduitCriteria setGammeId(Long gammeId) {
    this.gammeId = gammeId;
    return this;
  }

  public Long getFabriquantId() {
    return fabriquantId;
  }

  public ProduitCriteria setFabriquantId(Long fabriquantId) {
    this.fabriquantId = fabriquantId;
    return this;
  }

  public Long getLaboratoireId() {
    return laboratoireId;
  }

  public ProduitCriteria setLaboratoireId(Long laboratoireId) {
    this.laboratoireId = laboratoireId;
    return this;
  }

  public Long getTvaId() {
    return tvaId;
  }

  public ProduitCriteria setTvaId(Long tvaId) {
    this.tvaId = tvaId;
    return this;
  }

  public Long getMagasinId() {
    return magasinId;
  }

  public ProduitCriteria setMagasinId(Long magasinId) {
    this.magasinId = magasinId;
    return this;
  }

  public Long getRayonId() {
    return rayonId;
  }

  public ProduitCriteria setRayonId(Long rayonId) {
    this.rayonId = rayonId;
    return this;
  }

  public String getSearch() {
    return search;
  }

  public ProduitCriteria setSearch(String search) {
    this.search = search;
    return this;
  }

  public Boolean getDeconditionne() {
    return deconditionne;
  }

  public ProduitCriteria setDeconditionne(Boolean deconditionne) {
    this.deconditionne = deconditionne;
    return this;
  }

  public Long getRemiseId() {
    return remiseId;
  }

  public ProduitCriteria setRemiseId(Long remiseId) {
    this.remiseId = remiseId;
    return this;
  }

  public Long getTableauId() {
    return tableauId;
  }

  public ProduitCriteria setTableauId(Long tableauId) {
    this.tableauId = tableauId;
    return this;
  }

  public Long getStorageId() {
    return storageId;
  }

  public ProduitCriteria setStorageId(Long storageId) {
    this.storageId = storageId;
    return this;
  }

  public TypeProduit getTypeProduit() {
    return typeProduit;
  }

  public ProduitCriteria setTypeProduit(TypeProduit typeProduit) {
    this.typeProduit = typeProduit;
    return this;
  }

  public Long getTableauNot() {
    return tableauNot;
  }

  public ProduitCriteria setTableauNot(Long tableauNot) {
    this.tableauNot = tableauNot;
    return this;
  }

  public Long getRayonNot() {
    return rayonNot;
  }

  public ProduitCriteria setRayonNot(Long rayonNot) {
    this.rayonNot = rayonNot;
    return this;
  }

  public Long getRemiseNot() {
    return remiseNot;
  }

  public ProduitCriteria setRemiseNot(Long remiseNot) {
    this.remiseNot = remiseNot;
    return this;
  }

  @Override
  public String toString() {
    String sb =
        "ProduitCriteria{"
            + "id="
            + id
            + ", codeEan='"
            + codeEan
            + '\''
            + ", codeCip='"
            + codeCip
            + '\''
            + ", libelle='"
            + libelle
            + '\''
            + ", status="
            + status
            + ", dateperemption="
            + dateperemption
            + ", deconditionnable="
            + deconditionnable
            + ", qtySeuilMini="
            + qtySeuilMini
            + ", qtyAppro="
            + qtyAppro
            + ", parentId="
            + parentId
            + ", prixPaf="
            + prixPaf
            + ", prixUni="
            + prixUni
            + ", formeId="
            + formeId
            + ", familleId="
            + familleId
            + ", gammeId="
            + gammeId
            + ", fabriquantId="
            + fabriquantId
            + ", laboratoireId="
            + laboratoireId
            + ", tvaId="
            + tvaId
            + ", magasinId="
            + magasinId
            + ", rayonId="
            + rayonId
            + ", search='"
            + search
            + '\''
            + ", deconditionne="
            + deconditionne
            + ", remiseId="
            + remiseId
            + ", storageId="
            + storageId
            + ", typeProduit="
            + typeProduit
            + '}';
    return sb;
  }
}
