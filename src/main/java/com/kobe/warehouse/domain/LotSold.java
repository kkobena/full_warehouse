package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "lot_sold",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lot_id", "sale_line_id"})
    })
public class LotSold implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "lotSolds", allowSetters = true)
    private Lot lot;
    @ManyToOne(optional = false)
    private SalesLine saleLine;
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Long getId() {
        return id;
    }

    public LotSold setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LotSold setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public Lot getLot() {
        return lot;
    }

    public LotSold setLot(Lot lot) {
        this.lot = lot;
        return this;
    }

    public SalesLine getSaleLine() {
        return saleLine;
    }

    public LotSold setSaleLine(SalesLine saleLine) {
        this.saleLine = saleLine;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LotSold setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LotSold lotSold = (LotSold) o;
        return id.equals(lotSold.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
