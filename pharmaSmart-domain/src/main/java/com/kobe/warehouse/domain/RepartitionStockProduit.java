package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "repartition_stock_produit",
    indexes = {@Index(columnList = "type_repartition", name = "type_repartition_index")
    }
)
public class RepartitionStockProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "stock_produit_source_id", referencedColumnName = "id")
    private StockProduit stockProduitSource;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "stock_produit_destination_id", referencedColumnName = "id")
    private StockProduit stockProduitDestination;

    @NotNull
    @ManyToOne(optional = false)
    private AppUser user;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @NotNull
    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;

    @Column(name = "source_init_stock")
    private Integer sourceInitStock;

    @Column(name = "source_final_stock")
    private Integer sourceFinalStock;

    @NotNull
    @Column(name = "dest_init_stock", nullable = false)
    private Integer destInitStock;

    @NotNull
    @Column(name = "dest_final_stock", nullable = false)
    private Integer destFinalStock;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_repartition", nullable = false)
    private TypeRepartition typeRepartition;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public StockProduit getStockProduitSource() {
        return stockProduitSource;
    }

    public void setStockProduitSource(StockProduit stockProduitSource) {
        this.stockProduitSource = stockProduitSource;
    }

    public StockProduit getStockProduitDestination() {
        return stockProduitDestination;
    }

    public void setStockProduitDestination(StockProduit stockProduitDestination) {
        this.stockProduitDestination = stockProduitDestination;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Integer getSourceInitStock() {
        return sourceInitStock;
    }

    public void setSourceInitStock(Integer sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
    }

    public Integer getSourceFinalStock() {
        return sourceFinalStock;
    }

    public void setSourceFinalStock(Integer sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
    }

    public Integer getDestInitStock() {
        return destInitStock;
    }

    public void setDestInitStock(Integer destInitStock) {
        this.destInitStock = destInitStock;
    }

    public Integer getDestFinalStock() {
        return destFinalStock;
    }

    public void setDestFinalStock(Integer destFinalStock) {
        this.destFinalStock = destFinalStock;
    }

    public TypeRepartition getTypeRepartition() {
        return typeRepartition;
    }

    public void setTypeRepartition(TypeRepartition typeRepartition) {
        this.typeRepartition = typeRepartition;
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
