package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import java.io.Serializable;
import lombok.Getter;

@Getter
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

  public ProduitCriteria setId(Long id) {
    this.id = id;
    return this;
  }

  public ProduitCriteria setRemiseNot(Long remiseNot) {
    this.remiseNot = remiseNot;
    return this;
  }

  public ProduitCriteria setTableauId(Long tableauId) {
    this.tableauId = tableauId;
    return this;
  }

  public ProduitCriteria setTableauNot(Long tableauNot) {
    this.tableauNot = tableauNot;
    return this;
  }

  public ProduitCriteria setRayonNot(Long rayonNot) {
    this.rayonNot = rayonNot;
    return this;
  }

  public ProduitCriteria setStorageId(Long storageId) {
    this.storageId = storageId;
    return this;
  }

  public ProduitCriteria setCodeEan(String codeEan) {
    this.codeEan = codeEan;
    return this;
  }

  public ProduitCriteria setCodeCip(String codeCip) {
    this.codeCip = codeCip;
    return this;
  }

  public ProduitCriteria setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public ProduitCriteria setStatus(Status status) {
    this.status = status;
    return this;
  }

  public ProduitCriteria setDateperemption(Boolean dateperemption) {
    this.dateperemption = dateperemption;
    return this;
  }

  public ProduitCriteria setDeconditionnable(Boolean deconditionnable) {
    this.deconditionnable = deconditionnable;
    return this;
  }

  public ProduitCriteria setQtySeuilMini(Long qtySeuilMini) {
    this.qtySeuilMini = qtySeuilMini;
    return this;
  }

  public ProduitCriteria setQtyAppro(Integer qtyAppro) {
    this.qtyAppro = qtyAppro;
    return this;
  }

  public ProduitCriteria setParentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

  public ProduitCriteria setPrixPaf(Integer prixPaf) {
    this.prixPaf = prixPaf;
    return this;
  }

  public ProduitCriteria setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
    return this;
  }

  public ProduitCriteria setFormeId(Long formeId) {
    this.formeId = formeId;
    return this;
  }

  public ProduitCriteria setFamilleId(Long familleId) {
    this.familleId = familleId;
    return this;
  }

  public ProduitCriteria setGammeId(Long gammeId) {
    this.gammeId = gammeId;
    return this;
  }

  public ProduitCriteria setFabriquantId(Long fabriquantId) {
    this.fabriquantId = fabriquantId;
    return this;
  }

  public ProduitCriteria setLaboratoireId(Long laboratoireId) {
    this.laboratoireId = laboratoireId;
    return this;
  }

  public ProduitCriteria setTvaId(Long tvaId) {
    this.tvaId = tvaId;
    return this;
  }

  public ProduitCriteria setMagasinId(Long magasinId) {
    this.magasinId = magasinId;
    return this;
  }

  public ProduitCriteria setRayonId(Long rayonId) {
    this.rayonId = rayonId;
    return this;
  }

  public ProduitCriteria setSearch(String search) {
    this.search = search;
    return this;
  }

  public ProduitCriteria setDeconditionne(Boolean deconditionne) {
    this.deconditionne = deconditionne;
    return this;
  }

  public ProduitCriteria setRemiseId(Long remiseId) {
    this.remiseId = remiseId;
    return this;
  }

  public ProduitCriteria setTypeProduit(TypeProduit typeProduit) {
    this.typeProduit = typeProduit;
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
