package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "repartition_stock_produit")
public class RepartitionStockProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "stock_produit_source_id", referencedColumnName = "id")
   private StockProduit stockProduitSource;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "stock_produit_destination_id", referencedColumnName = "id")
   private StockProduit stockProduitDestination;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    private Produit produit;

    @NotNull
    @ManyToOne(optional = false)
    private AppUser user;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @NotNull
    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;

    @NotNull
    @Column(name = "source_init_stock", nullable = false)
    private Integer sourceInitStock;

    @NotNull
    @Column(name = "source_final_stock", nullable = false)
    private Integer sourceFinalStock;

    @NotNull
    @Column(name = "dest_init_stock", nullable = false)
    private Integer destInitStock;

    @NotNull
    @Column(name = "dest_final_stock", nullable = false)
    private Integer destFinalStock;

    public @NotNull StockProduit getStockProduitSource() {
        return stockProduitSource;
    }

    public RepartitionStockProduit setStockProduitSource(StockProduit stockProduitSource) {
        this.stockProduitSource = stockProduitSource;
        return this;
    }

    public @NotNull StockProduit getStockProduitDestination() {
        return stockProduitDestination;
    }

    public RepartitionStockProduit setStockProduitDestination(StockProduit stockProduitDestination) {
        this.stockProduitDestination = stockProduitDestination;
        return this;
    }

    public Long getId() {
        return id;
    }

    public RepartitionStockProduit setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public RepartitionStockProduit setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public RepartitionStockProduit setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public @NotNull LocalDateTime getCreated() {
        return created;
    }

    public RepartitionStockProduit setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public @NotNull Integer getQtyMvt() {
        return qtyMvt;
    }

    public RepartitionStockProduit setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public @NotNull Integer getSourceInitStock() {
        return sourceInitStock;
    }

    public RepartitionStockProduit setSourceInitStock(Integer sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
        return this;
    }

    public @NotNull Integer getSourceFinalStock() {
        return sourceFinalStock;
    }

    public RepartitionStockProduit setSourceFinalStock(Integer sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
        return this;
    }

    public @NotNull Integer getDestInitStock() {
        return destInitStock;
    }

    public RepartitionStockProduit setDestInitStock(Integer destInitStock) {
        this.destInitStock = destInitStock;
        return this;
    }

    public @NotNull Integer getDestFinalStock() {
        return destFinalStock;
    }

    public RepartitionStockProduit setDestFinalStock(Integer destFinalStock) {
        this.destFinalStock = destFinalStock;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepartitionStockProduit that = (RepartitionStockProduit) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
