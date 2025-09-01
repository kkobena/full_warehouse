package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

/**
 * A InventoryTransaction.
 */
@Entity
@Table(
    name = "inventory_transaction",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "entity_id", "produit_id", "mouvement_type", "transaction_date" }) },
    indexes = {
        @Index(columnList = "mouvement_type", name = "inventory_mouvement_type_type_index"),
        @Index(columnList = "transaction_date", name = "inventory_transaction_date_index"),
    }
)
@IdClass(ProductMvtId.class)
public class InventoryTransaction implements Persistable<ProductMvtId>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Id
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mouvement_type", nullable = false)
    private MouvementProduit mouvementType;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "quantity_befor", nullable = false)
    private Integer quantityBefor;

    @NotNull
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produit produit;

    @ManyToOne(optional = false)
    @NotNull
    private AppUser user;

    @NotNull
    @Column(name = "cost_amount", nullable = false)
    private Integer costAmount;

    @NotNull
    @Column(name = "regular_unit_price", nullable = false)
    private Integer regularUnitPrice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Magasin magasin;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Transient
    private boolean isNew = true;

    public ProductMvtId getId() {
        return new ProductMvtId(id, transactionDate);
    }

    public InventoryTransaction setId(Long id) {
        this.id = id;
        return this;
    }

    public MouvementProduit getMouvementType() {
        return mouvementType;
    }

    public InventoryTransaction setMouvementType(MouvementProduit transactionType) {
        this.mouvementType = transactionType;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public InventoryTransaction setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public InventoryTransaction setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Integer getQuantityBefor() {
        return quantityBefor;
    }

    public InventoryTransaction setQuantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
        return this;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public InventoryTransaction setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public InventoryTransaction setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public InventoryTransaction setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public InventoryTransaction setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public InventoryTransaction setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public InventoryTransaction setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
    }

    public Long getEntityId() {
        return entityId;
    }

    public InventoryTransaction setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InventoryTransaction)) {
            return false;
        }
        return id != null && id.equals(((InventoryTransaction) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
