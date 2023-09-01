package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.AjustType;
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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.Getter;

/**
 * A Ajustement.
 */
@Getter
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
    private LocalDateTime dateMtv;
    @NotNull
    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_ajust", nullable = false)
    private AjustType type;
    @NotNull
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "ajustements", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "ajustements", allowSetters = true)
    private Ajust ajust;
    @ManyToOne
    private MotifAjustement motifAjustement;

    public Ajustement setMotifAjustement(MotifAjustement motifAjustement) {
        this.motifAjustement = motifAjustement;
        return this;
    }

    public Ajustement setType(AjustType type) {
        this.type = type;
        return this;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Ajustement qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }

    public Ajustement dateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
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

    public void setAjust(Ajust ajust) {
        this.ajust = ajust;
    }
}
