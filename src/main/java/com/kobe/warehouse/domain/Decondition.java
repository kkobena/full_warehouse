package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** A Decondition. */
@Entity
@Table(name = "decondition")
public class Decondition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv;

    @NotNull
    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @NotNull
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @ManyToOne(optional = false)
    @NotNull
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnoreProperties(value = "deconditions", allowSetters = true)
    private Produit produit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_deconditionnement", nullable = false, length = 16)
    private TypeDeconditionnement typeDeconditionnement;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public @NotNull Integer getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }

    public @NotNull Integer getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public @NotNull Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull TypeDeconditionnement getTypeDeconditionnement() {
        return typeDeconditionnement;
    }

    public Decondition setTypeDeconditionnement(TypeDeconditionnement typeDeconditionnement) {
        this.typeDeconditionnement = typeDeconditionnement;
        return this;
    }

    public Decondition qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Decondition dateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Decondition stockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
        return this;
    }

    public Decondition stockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
        return this;
    }

    public Decondition user(AppUser user) {
        this.user = user;
        return this;
    }

    public Decondition produit(Produit produit) {
        this.produit = produit;
        return this;
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
        return (
            "Decondition{" +
            "id=" +
            getId() +
            ", qtyMvt=" +
            getQtyMvt() +
            ", dateMtv='" +
            getDateMtv() +
            "'" +
            ", stockBefore=" +
            getStockBefore() +
            ", stockAfter=" +
            getStockAfter() +
            "}"
        );
    }
}
