package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A InventoryTransaction.
 */
@Entity
@Table(
    name = "inventory_transaction",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "entity_id", "produit_id", "mouvemen_type" }) },
    indexes = {
        @Index(columnList = "mouvemen_type", name = "mouvemen_type_index"), @Index(columnList = "created_at", name = "createdAt_index"),
    }
)
public class InventoryTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mouvemen_type", nullable = false)
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
    private User user;

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

    public Long getId() {
        return id;
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

    public User getUser() {
        return user;
    }

    public InventoryTransaction setUser(User user) {
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
}
