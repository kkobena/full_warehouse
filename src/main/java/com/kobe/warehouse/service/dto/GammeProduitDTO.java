package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GammeProduit;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A DTO for the {@link com.kobe.warehouse.domain.GammeProduit} entity.
 */
public class GammeProduitDTO implements Serializable {
    private static final long serialVersionUID = -605218151933409039L;
    private Long id;
    private String code;
    @NotNull
    private String libelle;
    public Long getId() {
        return id;
    }

    public GammeProduitDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public GammeProduitDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public GammeProduitDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GammeProduitDTO)) {
            return false;
        }

        return id != null && id.equals(((GammeProduitDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GammeProduitDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            "}";
    }

    public GammeProduitDTO() {
    }

    public GammeProduitDTO(GammeProduit g) {
        this.id = g.getId();
        this.code = g.getCode();
        this.libelle = g.getLibelle();
    }
}
