package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
public class SalePayment extends PaymentTransaction implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        { @JoinColumn(name = "sale_id", referencedColumnName = "id"), @JoinColumn(name = "sale_date", referencedColumnName = "sale_date") }
    )
    private Sales sale;

    @NotNull
    @Column(name = "part_assure", columnDefinition = "int default '0'")
    private Integer partAssure;

    @NotNull
    @Column(name = "part_tiers_payant", columnDefinition = "int default '0'")
    private Integer partTiersPayant;

    public Sales getSale() {
        return sale;
    }

    public SalePayment setSale(Sales sale) {
        this.sale = sale;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public SalePayment setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public SalePayment setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Exception e) {
            return null;
        }
    }
}
