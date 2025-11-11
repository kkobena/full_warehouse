package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FormProduit;
import java.io.Serializable;

public class FormProduitDTO implements Serializable {

    private Integer id;

    private String libelle;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public FormProduitDTO(FormProduit formProduit) {
        this.id = formProduit.getId();
        this.libelle = formProduit.getLibelle();
    }

    public FormProduitDTO() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FormProduitDTO)) {
            return false;
        }

        return id != null && id.equals(((FormProduitDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FormProduitDTO{" +
            "id=" + getId() +

            ", libelle='" + getLibelle() + "'" +

            "}";
    }
}
