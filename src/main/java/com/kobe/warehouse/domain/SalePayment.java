package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
public class SalePayment extends PaymentTransaction implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    @ManyToOne
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
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
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
