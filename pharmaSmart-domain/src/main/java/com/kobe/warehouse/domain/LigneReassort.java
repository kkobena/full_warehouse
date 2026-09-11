package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "ligne_reassort", uniqueConstraints = {@UniqueConstraint(columnNames = {"reassort_id", "stock_produit_id"})})
public class LigneReassort implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "quantity")
    private Integer quantity;

    @NotNull
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(optional = false)
    private SuggestionReassort reassort;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stock_produit_id", referencedColumnName = "id")
    private StockProduit stockProduit;

    @ManyToOne
    @JoinColumn(name = "stock_src_produit_id", referencedColumnName = "id")
    private StockProduit stockProduitSrc;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SuggestionReassort getReassort() {
        return reassort;
    }

    public void setReassort(SuggestionReassort reassort) {
        this.reassort = reassort;
    }

    public StockProduit getStockProduit() {
        return stockProduit;
    }

    public void setStockProduit(StockProduit stockProduit) {
        this.stockProduit = stockProduit;
    }

    public StockProduit getStockProduitSrc() {
        return stockProduitSrc;
    }

    public LigneReassort setStockProduitSrc(StockProduit stockProduitSrc) {
        this.stockProduitSrc = stockProduitSrc;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LigneReassort that)) {
            return false;
        }
        // Deux lignes pas encore enregistrées ne sont pas la même ligne : les égaler par un id null
        // les confondait dans le Set de la suggestion, et une détection portant sur plusieurs
        // produits n'en gardait qu'un seul.
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
