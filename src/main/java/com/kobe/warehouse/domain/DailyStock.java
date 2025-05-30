package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "daily_stock", uniqueConstraints = { @UniqueConstraint(columnNames = { "date_key", "produit_id" }) })
public class DailyStock implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "date_key", nullable = false)
    private LocalDate key = LocalDate.now();

    @Column(name = "stock", nullable = false)
    @ColumnDefault(value = "0")
    private int stock;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Produit produit;

    public Produit getProduit() {
        return produit;
    }

    public DailyStock setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public DailyStock() {}

    public Long getId() {
        return id;
    }

    public DailyStock setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDate getKey() {
        return key;
    }

    public DailyStock setKey(LocalDate key) {
        this.key = key;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DailyStock that = (DailyStock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
