package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Tva;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class TvaDTO implements Serializable {
    private static final long serialVersionUID = -656800366874873921L;
    private Long id;
    @NotNull
    private Integer taux;
    private String tva;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
        this.tva = taux.toString();
        return tva;
    }


    @Override
    public String toString() {
        return "TvaDTO{" +
            "id=" + getId() +
            ", taux=" + getTaux() +
            "}";
    }

    public TvaDTO(Tva tva) {
        this.id = tva.getId();
        this.taux = tva.getTaux();

    }

    public TvaDTO() {
    }
}
