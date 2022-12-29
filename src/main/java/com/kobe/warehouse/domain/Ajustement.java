package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import java.io.Serializable;
import java.time.Instant;


/**
 * A Ajustement.
 */
@Entity
@Table(name = "ajustement", uniqueConstraints = {@UniqueConstraint(columnNames = {"ajust_id", "produit_id"})})
public class Ajustement implements Serializable {

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
    @JsonIgnoreProperties(value = "ajustements", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @NotNull
    private Ajust ajust;
    @ManyToOne
    private MotifAjustement motifAjustement;

    public MotifAjustement getMotifAjustement() {
        return motifAjustement;
    }

    public Ajustement setMotifAjustement(MotifAjustement motifAjustement) {
        this.motifAjustement = motifAjustement;
        return this;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Ajustement qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Instant getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
    }

    public Ajustement dateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Ajustement produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ajustement)) {
            return false;
        }
        return id != null && id.equals(((Ajustement) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Ajustement{" +
            "id=" + getId() +
            ", qtyMvt=" + getQtyMvt() +
            ", dateMtv='" + getDateMtv() + "'" +
            "}";
    }

    public Ajust getAjust() {
        return ajust;
    }

    public void setAjust(Ajust ajust) {
        this.ajust = ajust;
    }
}
