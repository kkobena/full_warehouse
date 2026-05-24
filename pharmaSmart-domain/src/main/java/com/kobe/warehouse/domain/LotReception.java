package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Trace chaque réception d'un lot, quelle que soit la commande d'origine.
 * Permet la traçabilité complète : un même lot peut être livré sur plusieurs
 * bons de commande (livraisons partielles, réassorts du même fabricant).
 */
@Entity
@Table(
    name = "lot_reception",
    indexes = {
        @Index(columnList = "lot_id", name = "lot_reception_lot_id_idx"),
        @Index(columnList = "order_line_id, commande_order_date", name = "lot_reception_order_line_idx"),
    }
)
public class LotReception implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "order_line_id", referencedColumnName = "id"),
        @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date"),
    })
    private OrderLine orderLine;

    @NotNull
    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "free_qty", nullable = false)
    private int freeQty;

    @NotNull
    @Column(name = "prix_achat", nullable = false)
    private Integer prixAchat;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public Lot getLot() {
        return lot;
    }

    public LotReception setLot(Lot lot) {
        this.lot = lot;
        return this;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public LotReception setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
        return this;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public LotReception setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public int getFreeQty() {
        return freeQty;
    }

    public LotReception setFreeQty(int freeQty) {
        this.freeQty = freeQty;
        return this;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public LotReception setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public LotReception setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LotReception setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
