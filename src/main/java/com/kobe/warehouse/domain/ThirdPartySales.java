package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ThirdPartySales extends Sales implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "num_bon", length = 50)
    private String numBon;

    @ManyToOne
    private AssuredCustomer ayantDroit;

    @Column(name = "part_assure", columnDefinition = "int default '0'")
    private Integer partAssure;

    @Column(name = "part_tiers_payant", columnDefinition = "int default '0'")
    private Integer partTiersPayant;

    @OneToMany(
        mappedBy = "sale",
        orphanRemoval = true,
        cascade = {CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE})
    private List<ThirdPartySaleLine> thirdPartySaleLines = new ArrayList<>();

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

    public List<ThirdPartySaleLine> getThirdPartySaleLines() {
        return thirdPartySaleLines;
    }

    public ThirdPartySales setThirdPartySaleLines(List<ThirdPartySaleLine> thirdPartySaleLines) {
        this.thirdPartySaleLines = thirdPartySaleLines;
        return this;
    }
}
