package com.kobe.warehouse.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "repartition_stock_produit")

public class RepartitionStockProduit implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @ManyToOne(optional = false)
    @NotNull
    private Produit produit;
    @NotNull
    @ManyToOne(optional = false)
    private User user;
    @ManyToOne(optional = false)
    @NotNull
    StockProduit stockProduitSource;
    @ManyToOne(optional = false)
    @NotNull
    StockProduit stockProduitDestination;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant created=Instant.now();
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

    public Long getId() {
        return id;
    }

    public RepartitionStockProduit setId(Long id) {
        this.id = id;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public RepartitionStockProduit setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public User getUser() {
        return user;
    }

    public RepartitionStockProduit setUser(User user) {
        this.user = user;
        return this;
    }

    public StockProduit getStockProduitSource() {
        return stockProduitSource;
    }

    public RepartitionStockProduit setStockProduitSource(StockProduit stockProduitSource) {
        this.stockProduitSource = stockProduitSource;
        return this;
    }

    public StockProduit getStockProduitDestination() {
        return stockProduitDestination;
    }

    public RepartitionStockProduit setStockProduitDestination(StockProduit stockProduitDestination) {
        this.stockProduitDestination = stockProduitDestination;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public RepartitionStockProduit setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public RepartitionStockProduit setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Integer getSourceInitStock() {
        return sourceInitStock;
    }

    public RepartitionStockProduit setSourceInitStock(Integer sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
        return this;
    }

    public Integer getSourceFinalStock() {
        return sourceFinalStock;
    }

    public RepartitionStockProduit setSourceFinalStock(Integer sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
        return this;
    }

    public Integer getDestInitStock() {
        return destInitStock;
    }

    public RepartitionStockProduit setDestInitStock(Integer destInitStock) {
        this.destInitStock = destInitStock;
        return this;
    }

    public Integer getDestFinalStock() {
        return destFinalStock;
    }

    public RepartitionStockProduit setDestFinalStock(Integer destFinalStock) {
        this.destFinalStock = destFinalStock;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepartitionStockProduit that = (RepartitionStockProduit) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
