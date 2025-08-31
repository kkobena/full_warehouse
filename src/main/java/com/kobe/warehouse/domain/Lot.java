package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "lot",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "num_lot", "order_line_id" }) },
    indexes = { @Index(columnList = "num_lot", name = "num_lot_index") }
)
public class Lot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "num_lot", nullable = false)
    private String numLot;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private OrderLine orderLine;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quantity_received_ug", nullable = false)
    private int freeQty;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated")
    private LocalDateTime updated = LocalDateTime.now();

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @NotNull
    private Integer prixAchat;

    @NotNull
    private Integer prixUnit;

    public int getFreeQty() {
        return freeQty;
    }

    public Lot setFreeQty(int ugQuantityReceived) {
        this.freeQty = ugQuantityReceived;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Long getId() {
        return id;
    }

    public Lot setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public Lot setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public Lot setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Lot setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Lot setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public Lot setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
        return this;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public Lot setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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
        Lot lot = (Lot) o;
        return id.equals(lot.id);
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public Integer getPrixUnit() {
        return prixUnit;
    }

    public void setPrixUnit(Integer prixUnit) {
        this.prixUnit = prixUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
