package com.kobe.warehouse.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Entity
public class ThirdPartySales extends Sales implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "num_bon", length = 50)
    private String numBon;
    @ManyToOne
    private AssuredCustomer ayantDroit;
    @Column(name = "part_assure")
    private Integer partAssure;
    @Column(name = "part_tiers_payant")
    private Integer partTiersPayant;

    public String getNumBon() {
        return numBon;
    }

    public ThirdPartySales setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public AssuredCustomer getAyantDroit() {
        return ayantDroit;
    }

    public ThirdPartySales setAyantDroit(AssuredCustomer ayantDroit) {
        this.ayantDroit = ayantDroit;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public ThirdPartySales setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public ThirdPartySales setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }
}
