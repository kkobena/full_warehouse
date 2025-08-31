package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.AjustType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A Ajustement.
 */
@Entity
@Table(name = "ajustement", uniqueConstraints = { @UniqueConstraint(columnNames = { "ajust_id", "produit_id" }) })
public class Ajustement implements Serializable {

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
    @Enumerated(EnumType.STRING)
    @Column(name = "type_ajust", nullable = false)
    private AjustType type;

    @NotNull
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "ajustements", allowSetters = true)
    private Produit produit;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnoreProperties(value = "ajustements", allowSetters = true)
    private Ajust ajust;

    @ManyToOne
    @JoinColumn(name = "motif_ajustement_id", referencedColumnName = "id")
    private MotifAjustement motifAjustement;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public @NotNull AjustType getType() {
        return type;
    }

    public Ajustement setType(AjustType type) {
        this.type = type;
        return this;
    }

    public @NotNull Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull Ajust getAjust() {
        return ajust;
    }

    public void setAjust(Ajust ajust) {
        this.ajust = ajust;
    }

    public MotifAjustement getMotifAjustement() {
        return motifAjustement;
    }

    public Ajustement setMotifAjustement(MotifAjustement motifAjustement) {
        this.motifAjustement = motifAjustement;
        return this;
    }

    public Ajustement qtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Ajustement dateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
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
        return "Ajustement{"
            + "id="
            + getId()
            + ", qtyMvt="
            + getQtyMvt()
            + ", dateMtv='"
            + getDateMtv()
            + "'"
            + "}";
    }
}
