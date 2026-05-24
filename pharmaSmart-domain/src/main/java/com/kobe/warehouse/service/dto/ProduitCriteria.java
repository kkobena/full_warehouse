package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import java.io.Serializable;

public class ProduitCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String codeEan;
    private String codeCip;
    private String libelle;
    private Status status;
    private Boolean dateperemption;
    private Boolean deconditionnable;
    private Long qtySeuilMini;
    private Integer qtyAppro;
    private Integer parentId;
    private Integer prixPaf;
    private Integer prixUni;
    private Integer formeId;
    private Integer familleId;
    private Integer gammeId;
    private Integer fabriquantId;
    private Integer laboratoireId;
    private Integer tvaId;
    private Integer magasinId;
    private Integer rayonId;
    private String search;
    private Boolean deconditionne;
    private Boolean remisable;
    private Integer tableauId;
    private Integer storageId;
    private TypeProduit typeProduit;
    private Integer tableauNot;
    private Integer rayonNot;
    private boolean depot;

    public Integer getId() {
        return id;
    }

    public ProduitCriteria setId(Integer id) {
        this.id = id;
        return this;
    }

    public boolean isDepot() {
        return depot;
    }

    public ProduitCriteria setDepot(boolean depot) {
        this.depot = depot;
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

    public Integer getParentId() {
        return parentId;
    }

    public ProduitCriteria setParentId(Integer parentId) {
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

    public Integer getFormeId() {
        return formeId;
    }

    public ProduitCriteria setFormeId(Integer formeId) {
        this.formeId = formeId;
        return this;
    }

    public Integer getFamilleId() {
        return familleId;
    }

    public ProduitCriteria setFamilleId(Integer familleId) {
        this.familleId = familleId;
        return this;
    }

    public Integer getGammeId() {
        return gammeId;
    }

    public ProduitCriteria setGammeId(Integer gammeId) {
        this.gammeId = gammeId;
        return this;
    }

    public Integer getFabriquantId() {
        return fabriquantId;
    }

    public ProduitCriteria setFabriquantId(Integer fabriquantId) {
        this.fabriquantId = fabriquantId;
        return this;
    }

    public Integer getLaboratoireId() {
        return laboratoireId;
    }

    public ProduitCriteria setLaboratoireId(Integer laboratoireId) {
        this.laboratoireId = laboratoireId;
        return this;
    }

    public Integer getTvaId() {
        return tvaId;
    }

    public ProduitCriteria setTvaId(Integer tvaId) {
        this.tvaId = tvaId;
        return this;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    public ProduitCriteria setMagasinId(Integer magasinId) {
        this.magasinId = magasinId;
        return this;
    }

    public Integer getRayonId() {
        return rayonId;
    }

    public ProduitCriteria setRayonId(Integer rayonId) {
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

    public Integer getTableauId() {
        return tableauId;
    }

    public ProduitCriteria setTableauId(Integer tableauId) {
        this.tableauId = tableauId;
        return this;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public ProduitCriteria setStorageId(Integer storageId) {
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

    public Integer getTableauNot() {
        return tableauNot;
    }

    public ProduitCriteria setTableauNot(Integer tableauNot) {
        this.tableauNot = tableauNot;
        return this;
    }

    public Integer getRayonNot() {
        return rayonNot;
    }

    public ProduitCriteria setRayonNot(Integer rayonNot) {
        this.rayonNot = rayonNot;
        return this;
    }

    public Boolean getRemisable() {
        return remisable;
    }

    public ProduitCriteria setRemisable(Boolean remisable) {
        this.remisable = remisable;
        return this;
    }

    @Override
    public String toString() {
        String sb =
            "ProduitCriteria{" +
            "id=" +
            id +
            ", codeEan='" +
            codeEan +
            '\'' +
            ", codeCip='" +
            codeCip +
            '\'' +
            ", libelle='" +
            libelle +
            '\'' +
            ", status=" +
            status +
            ", dateperemption=" +
            dateperemption +
            ", deconditionnable=" +
            deconditionnable +
            ", qtySeuilMini=" +
            qtySeuilMini +
            ", qtyAppro=" +
            qtyAppro +
            ", parentId=" +
            parentId +
            ", prixPaf=" +
            prixPaf +
            ", prixUni=" +
            prixUni +
            ", formeId=" +
            formeId +
            ", familleId=" +
            familleId +
            ", gammeId=" +
            gammeId +
            ", fabriquantId=" +
            fabriquantId +
            ", laboratoireId=" +
            laboratoireId +
            ", tvaId=" +
            tvaId +
            ", magasinId=" +
            magasinId +
            ", rayonId=" +
            rayonId +
            ", search='" +
            search +
            '\'' +
            ", deconditionne=" +
            deconditionne +
            ", remiseId=" +
            ", storageId=" +
            storageId +
            ", typeProduit=" +
            typeProduit +
            '}';
        return sb;
    }
}
