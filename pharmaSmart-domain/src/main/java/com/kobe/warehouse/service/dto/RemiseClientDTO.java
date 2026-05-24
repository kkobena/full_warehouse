package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RemiseClient;
import jakarta.validation.constraints.NotNull;

public class RemiseClientDTO extends RemiseDTO {

    private static final long serialVersionUID = -2857904340237832912L;

    @NotNull
    private Float remiseValue;

    public RemiseClientDTO() {}

    public RemiseClientDTO(RemiseClient remise) {
        super(remise);
        this.setTypeLibelle("Remise client");
        this.remiseValue = remise.getRemiseValue();
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public void setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
    }
}
