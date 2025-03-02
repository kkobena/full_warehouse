package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.TypeEtiquette;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class TypeEtiquetteDTO implements Serializable {

    private Long id;

    @NotNull
    private String libelle;

    public TypeEtiquetteDTO() {}

    public TypeEtiquetteDTO(TypeEtiquette typeEtiquette) {
        id = typeEtiquette.getId();
        libelle = typeEtiquette.getLibelle();
    }

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
}
