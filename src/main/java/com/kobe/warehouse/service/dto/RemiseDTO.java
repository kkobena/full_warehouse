package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kobe.warehouse.domain.Remise;
import java.io.Serial;
import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = RemiseClientDTO.class, name = "remiseClient"),
        @JsonSubTypes.Type(value = RemiseProduitDTO.class, name = "remiseProduit"),
    }
)
public class RemiseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected Long id;

    protected String valeur;

    protected String typeRemise;
    protected String typeLibelle;
    protected String displayName;

    protected boolean enable = true;

    public RemiseDTO() {}

    public RemiseDTO(Remise remise) {
        id = remise.getId();
        valeur = remise.getValeur();

        displayName = remise.getValeur();
        this.enable = remise.isEnable();
    }

    public boolean isEnable() {
        return enable;
    }

    public RemiseDTO setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public String getTypeRemise() {
        return typeRemise;
    }

    public void setTypeRemise(String typeRemise) {
        this.typeRemise = typeRemise;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemiseDTO)) {
            return false;
        }

        return id != null && id.equals(((RemiseDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
