package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * A Decondition.
 */
@Entity
@Table(name = "decondition")
public class Decondition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private Instant dateMtv;

    @NotNull
    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @NotNull
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "deconditions", allowSetters = true)
    private Produit produit;

    @ManyToOne(optional = false)
    @NotNull
    private DateDimension dateDimension;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public Decondition qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Instant getDateMtv() {
        return dateMtv;
    }

    public Decondition dateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public void setDateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public Decondition stockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
        return this;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public Decondition stockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
        return this;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public User getUser() {
        return user;
    }

    public Decondition user(User user) {
        this.user = user;
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Produit getProduit() {
        return produit;
    }

    public Decondition produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public Decondition dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Decondition)) {
            return false;
        }
        return id != null && id.equals(((Decondition) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }


    @Override
    public String toString() {
        return "Decondition{" +
            "id=" + getId() +
            ", qtyMvt=" + getQtyMvt() +
            ", dateMtv='" + getDateMtv() + "'" +
            ", stockBefore=" + getStockBefore() +
            ", stockAfter=" + getStockAfter() +
            "}";
    }
}
