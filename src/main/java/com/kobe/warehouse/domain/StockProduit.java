package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * A StockProduit.
 */
@Entity
@Table(name = "stock_produit", uniqueConstraints = @UniqueConstraint(columnNames = { "storage_id", "produit_id" }))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
public class StockProduit implements Serializable {

    @Serial
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
    @Min(0)
    @Column(name = "qty_ug", nullable = false)
    private Integer qtyUG = 0;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "stockProduits", allowSetters = true)
    private Produit produit;

    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;

    @NotAudited
    @Formula("qty_ug+qty_stock")
    private Integer totalStockQuantity;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull Integer getQtyStock() {
        return qtyStock;
    }

    public void setQtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
    }

    public @NotNull Integer getQtyVirtual() {
        return qtyVirtual;
    }

    public void setQtyVirtual(Integer qtyVirtual) {
        this.qtyVirtual = qtyVirtual;
    }

    public @NotNull @Min(0) Integer getQtyUG() {
        return qtyUG;
    }

    public void setQtyUG(Integer qtyUG) {
        this.qtyUG = qtyUG;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull Storage getStorage() {
        return storage;
    }

    public StockProduit setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public Integer getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public StockProduit setTotalStockQuantity(Integer totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
        return this;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public StockProduit setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    public StockProduit qtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
        return this;
    }

    public StockProduit qtyVirtual(Integer qtyVirtual) {
        this.qtyVirtual = qtyVirtual;
        return this;
    }

    public StockProduit qtyUG(Integer qtyUG) {
        this.qtyUG = qtyUG;
        return this;
    }

    public StockProduit createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public StockProduit updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
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
        return "StockProduit{"
            + "id="
            + getId()
            + ", qtyStock="
            + getQtyStock()
            + ", qtyVirtual="
            + getQtyVirtual()
            + ", qtyUG="
            + getQtyUG()
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()
            + "'"
            + "}";
    }
}
