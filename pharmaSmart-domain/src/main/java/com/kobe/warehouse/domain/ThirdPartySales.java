package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;

@Entity
public class ThirdPartySales extends Sales implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "num_bon", length = 50)
    private String numBon;

    @ManyToOne
    @JoinColumn(name = "ayant_droit_id", referencedColumnName = "id")
    private AssuredCustomer ayantDroit;

    @Column(name = "part_assure", columnDefinition = "int default '0'")
    private Integer partAssure = 0;

    @Column(name = "part_tiers_payant", columnDefinition = "int default '0'")
    private Integer partTiersPayant = 0;

    /** Total des lignes exclues de la prise en charge (colonne « NR » du panier). */
    @Column(name = "montant_non_rembourse", nullable = false)
    private int montantNonRembourse;

    @ColumnDefault("false")
    @Column(name = "has_price_option")
    private boolean hasPriceOption;

    @OneToMany(mappedBy = "sale", orphanRemoval = true, cascade = { CascadeType.REMOVE })
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

    public int getMontantNonRembourse() {
        return montantNonRembourse;
    }

    public ThirdPartySales setMontantNonRembourse(int montantNonRembourse) {
        this.montantNonRembourse = montantNonRembourse;
        return this;
    }

    /** Montant de la vente sur lequel le tiers payant est sollicité : hors lignes non remboursées. */
    public int getMontantVenteRembourse() {
        return Objects.requireNonNullElse(getSalesAmount(), 0) - montantNonRembourse;
    }

    /** Part du patient sur la seule partie remboursable : ce que le tiers payant a besoin de voir sur la facture. */
    public int getPartAssureRembourse() {
        return Math.max(Objects.requireNonNullElse(partAssure, 0) - montantNonRembourse, 0);
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

    public boolean isHasPriceOption() {
        return hasPriceOption;
    }

    public ThirdPartySales setHasPriceOption(boolean hasPriceOption) {
        this.hasPriceOption = hasPriceOption;
        return this;
    }
}
