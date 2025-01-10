package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Entity
public class RemiseClient extends Remise implements Serializable {

    @Column(name = "remise_value")
    private Float remiseValue;

    @Transient
    private float tauxRemise;

    public float getTauxRemise() {
        if (Objects.nonNull(remiseValue)) {
            tauxRemise = remiseValue / 100;
        }
        return tauxRemise;
    }

    public RemiseClient setTauxRemise(float tauxRemise) {
        this.tauxRemise = tauxRemise;
        return this;
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public RemiseClient setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
        return this;
    }
}
