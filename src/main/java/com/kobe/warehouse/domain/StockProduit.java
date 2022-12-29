package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;


/**
 * A StockProduit.
 */
@Entity
@Table(name = "stock_produit",
    uniqueConstraints =
    @UniqueConstraint(columnNames = {"storage_id", "produit_id"})
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)

public class StockProduit implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Column(name = "qty_stock", nullable = false)
    private Integer qtyStock;
    @NotNull
    @Column(name = "qty_virtual", nullable = false)
    private Integer qtyVirtual;
    @NotNull
    @Column(name = "qty_ug", nullable = false)
    private Integer qtyUG;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "stockProduits", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;

    @Formula("qty_ug+qty_stock")
    private Integer totalStockQuantity;

    public Integer getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public StockProduit setTotalStockQuantity(Integer totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
        return this;
    }

    public Storage getStorage() {
        return storage;
    }

    public StockProduit setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQtyStock() {
        return qtyStock;
    }

    public void setQtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
    }

    public StockProduit qtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
        return this;
    }

    public Integer getQtyVirtual() {
        return qtyVirtual;
    }

    public void setQtyVirtual(Integer qtyVirtual) {
        this.qtyVirtual = qtyVirtual;
    }

    public StockProduit qtyVirtual(Integer qtyVirtual) {
        this.qtyVirtual = qtyVirtual;
        return this;
    }

    public Integer getQtyUG() {
        return qtyUG;
    }

    public void setQtyUG(Integer qtyUG) {
        this.qtyUG = qtyUG;
    }

    public StockProduit qtyUG(Integer qtyUG) {
        this.qtyUG = qtyUG;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public StockProduit createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StockProduit updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public StockProduit produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockProduit)) {
            return false;
        }
        return id != null && id.equals(((StockProduit) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StockProduit{" +
            "id=" + getId() +
            ", qtyStock=" + getQtyStock() +
            ", qtyVirtual=" + getQtyVirtual() +
            ", qtyUG=" + getQtyUG() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
