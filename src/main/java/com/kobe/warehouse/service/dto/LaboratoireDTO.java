package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Laboratoire;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A DTO for the {@link com.kobe.warehouse.domain.Laboratoire} entity.
 */
public class LaboratoireDTO implements Serializable {

    private Integer id;

    @NotNull
    private String libelle;

    public LaboratoireDTO() {}

    public LaboratoireDTO(Laboratoire laboratoire) {
        id = laboratoire.getId();
        libelle = laboratoire.getLibelle();
    }

    public Integer getId() {
        return id;
    }

    public LaboratoireDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public LaboratoireDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LaboratoireDTO)) {
            return false;
        }

        return id != null && id.equals(((LaboratoireDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "LaboratoireDTO{" + "id=" + getId() + ", libelle='" + getLibelle() + "'" + "}";
    }
}
