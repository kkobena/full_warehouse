package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.TypeEtiquette;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class TypeEtiquetteDTO implements Serializable {
    private Long id;
    @NotNull
    private String libelle;

    public Long getId() {
        return id;
    }

    public TypeEtiquetteDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public TypeEtiquetteDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeEtiquetteDTO)) {
            return false;
        }

        return id != null && id.equals(((TypeEtiquetteDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TypeEtiquetteDTO{" +
            "id=" + getId() +
            ", libelle='" + getLibelle() + "'" +

            "}";
    }

    public TypeEtiquetteDTO() {
    }

    public TypeEtiquetteDTO(TypeEtiquette typeEtiquette) {
        this.id = typeEtiquette.getId();
        this.libelle = typeEtiquette.getLibelle();
    }
}
