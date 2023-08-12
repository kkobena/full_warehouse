package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Entity
@Table(name = "repartition_stock_produit")

public class RepartitionStockProduit implements Serializable {
    private static final long serialVersionUID = 1L;
    @ManyToOne(optional = false)
    @NotNull
    StockProduit stockProduitSource;
    @ManyToOne(optional = false)
    @NotNull
    StockProduit stockProduitDestination;
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

  public RepartitionStockProduit setId(Long id) {
        this.id = id;
        return this;
    }

  public RepartitionStockProduit setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

  public RepartitionStockProduit setUser(User user) {
        this.user = user;
        return this;
    }

  public RepartitionStockProduit setStockProduitSource(StockProduit stockProduitSource) {
        this.stockProduitSource = stockProduitSource;
        return this;
    }

  public RepartitionStockProduit setStockProduitDestination(StockProduit stockProduitDestination) {
        this.stockProduitDestination = stockProduitDestination;
        return this;
    }

  public RepartitionStockProduit setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public RepartitionStockProduit setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

  public RepartitionStockProduit setSourceInitStock(Integer sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
        return this;
    }

  public RepartitionStockProduit setSourceFinalStock(Integer sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
        return this;
    }

  public RepartitionStockProduit setDestInitStock(Integer destInitStock) {
        this.destInitStock = destInitStock;
        return this;
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
