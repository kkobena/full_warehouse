package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Categorie;
import com.kobe.warehouse.domain.FamilleProduit;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class FamilleProduitDTO implements Serializable {

    private Long id;


    private String code;

    @NotNull
    private String libelle;

    private Long categorieId;

    private String categorieLibelle;

    public FamilleProduitDTO(FamilleProduit familleProduit) {
        id = familleProduit.getId();
        code = familleProduit.getCode();
        libelle = familleProduit.getLibelle();
        Categorie categorie = familleProduit.getCategorie();
        if (categorie != null) {
            categorieId = categorie.getId();
            categorieLibelle = categorie.getLibelle();
        }

    }

    public FamilleProduitDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieProduitId) {
        categorieId = categorieProduitId;
    }

    public String getCategorieLibelle() {
        return categorieLibelle;
    }

    public void setCategorieLibelle(String categorieProduitLibelle) {
        categorieLibelle = categorieProduitLibelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FamilleProduitDTO)) {
            return false;
        }

        return id != null && id.equals(((FamilleProduitDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FamilleProduitDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            ", categorieId=" + getCategorieId() +
            ", categorieLibelle='" + getCategorieLibelle() + "'" +
            "}";
    }
}
