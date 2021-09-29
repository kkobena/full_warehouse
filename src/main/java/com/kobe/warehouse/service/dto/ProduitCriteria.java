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
    private Long storageId;
    private TypeProduit typeProduit;
    public Long getId() {
        return id;
    }

    public ProduitCriteria setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getStorageId() {
        return storageId;
    }

    public ProduitCriteria setStorageId(Long storageId) {
        this.storageId = storageId;
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

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public ProduitCriteria setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
        return this;
    }

    public ProduitCriteria setRemiseId(Long remiseId) {
        this.remiseId = remiseId;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ProduitCriteria{");
        sb.append("id=").append(id);
        sb.append(", codeEan='").append(codeEan).append('\'');
        sb.append(", codeCip='").append(codeCip).append('\'');
        sb.append(", libelle='").append(libelle).append('\'');
        sb.append(", status=").append(status);
        sb.append(", dateperemption=").append(dateperemption);
        sb.append(", deconditionnable=").append(deconditionnable);
        sb.append(", qtySeuilMini=").append(qtySeuilMini);
        sb.append(", qtyAppro=").append(qtyAppro);
        sb.append(", parentId=").append(parentId);
        sb.append(", prixPaf=").append(prixPaf);
        sb.append(", prixUni=").append(prixUni);
        sb.append(", formeId=").append(formeId);
        sb.append(", familleId=").append(familleId);
        sb.append(", gammeId=").append(gammeId);
        sb.append(", fabriquantId=").append(fabriquantId);
        sb.append(", laboratoireId=").append(laboratoireId);
        sb.append(", tvaId=").append(tvaId);
        sb.append(", magasinId=").append(magasinId);
        sb.append(", rayonId=").append(rayonId);
        sb.append(", search='").append(search).append('\'');
        sb.append(", deconditionne=").append(deconditionne);
        sb.append(", remiseId=").append(remiseId);
        sb.append(", storageId=").append(storageId);
        sb.append(", typeProduit=").append(typeProduit);
        sb.append('}');
        return sb.toString();
    }
}
