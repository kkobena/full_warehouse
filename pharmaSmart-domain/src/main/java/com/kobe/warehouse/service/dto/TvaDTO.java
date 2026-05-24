package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Tva;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class TvaDTO implements Serializable {

    private static final long serialVersionUID = -656800366874873921L;
    private Integer id;

    @NotNull
    private Integer taux;

    private String tva;

    public TvaDTO(Tva tva) {
        id = tva.getId();
        taux = tva.getTaux();
    }

    public TvaDTO() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaux() {
        return taux;
    }

    public void setTaux(Integer taux) {
        this.taux = taux;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TvaDTO)) {
            return false;
        }

        return id != null && id.equals(((TvaDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public String getTva() {
        tva = taux.toString();
        return tva;
    }

    @Override
    public String toString() {
        return "TvaDTO{" + "id=" + getId() + ", taux=" + getTaux() + "}";
    }
}
