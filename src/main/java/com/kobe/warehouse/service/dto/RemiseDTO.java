package com.kobe.warehouse.service.dto;


import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.enumeration.Status;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;


public class RemiseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String valeur;

    @NotNull
    private Float remiseValue;

    private String typeRemise, typeLibelle;

    private Status status;

    public RemiseDTO() {
    }

    public RemiseDTO(Remise remise) {
        id = remise.getId();
        valeur = remise.getValeur();
        remiseValue = remise.getRemiseValue();
        status = remise.getStatus();
        if (remise instanceof RemiseClient) {
            typeRemise = "RC";
            typeLibelle = "Remise client";
        } else if (remise instanceof RemiseProduit) {
            typeRemise = "RP";
            typeLibelle = "Remise produit";
        }

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

    public Float getRemiseValue() {
        return remiseValue;
    }

    public void setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "RemiseDTO{" + "id=" + getId() + ", valeur='" + getValeur() + "'" + ", remiseValue=" + getRemiseValue()
            + ", status='" + getStatus() + "'" + "}";
    }
}
