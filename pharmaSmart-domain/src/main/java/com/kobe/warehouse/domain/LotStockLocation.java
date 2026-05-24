package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Table(
    name = "lot_stock_location",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "storage_id"}),
    indexes = {
        @Index(columnList = "lot_id",     name = "idx_lsl_lot"),
        @Index(columnList = "storage_id", name = "idx_lsl_storage"),
    }
)
public class LotStockLocation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_id", nullable = false)
    private Storage storage;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer qty;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();


    public LotStockLocation() {}

    public LotStockLocation(Lot lot, Storage storage, int qty) {
        this.lot     = lot;
        this.storage = storage;
        this.qty     = qty;
    }


    public Long getId() { return id; }

    public Lot getLot() { return lot; }
    public LotStockLocation setLot(Lot lot) { this.lot = lot; return this; }

    public Storage getStorage() { return storage; }
    public LotStockLocation setStorage(Storage storage) { this.storage = storage; return this; }

    public Integer getQty() { return qty; }
    public LotStockLocation setQty(Integer qty) {
        this.qty = qty;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
