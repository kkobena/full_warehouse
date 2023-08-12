package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;

/**
 * A Decondition.
 */
@Getter
@Entity
@Table(name = "decondition")
public class Decondition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "deconditions", allowSetters = true)
    private Produit produit;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_deconditionnement", nullable = false)
    private TypeDeconditionnement typeDeconditionnement;
    @ManyToOne(optional = false)
    @NotNull
    private DateDimension dateDimension;

    public void setId(Long id) {
        this.id = id;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Decondition qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Decondition setTypeDeconditionnement(TypeDeconditionnement typeDeconditionnement) {
        this.typeDeconditionnement = typeDeconditionnement;
        return this;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }

    public Decondition dateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Decondition stockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
        return this;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public Decondition stockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Decondition user(User user) {
        this.user = user;
        return this;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Decondition produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public Decondition dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
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
        return "Decondition{" +
            "id=" + getId() +
            ", qtyMvt=" + getQtyMvt() +
            ", dateMtv='" + getDateMtv() + "'" +
            ", stockBefore=" + getStockBefore() +
            ", stockAfter=" + getStockAfter() +
            "}";
    }
}
