package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "produit_perime", indexes = { @Index(columnList = "peremption_date", name = "produit_perime_index") })
public class ProduitPerime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @NotNull
    private Produit produit;

    @ManyToOne
    private Lot lot;

    @Column(name = "created", nullable = false)
    @NotNull
    private LocalDateTime created = LocalDateTime.now();

    @Min(1)
    private int quantity;

    @NotNull
    @Column(name = "peremption_date", nullable = false)
    private LocalDate peremptionDate;

    @ManyToOne(optional = false)
    @NotNull
    private AppUser user;

    @Min(1)
    @NotNull
    @Column(name = "init_stock", nullable = false)
    private int initStock;

    @NotNull
    @Column(name = "after_stock", nullable = false)
    private int afterStock;

    public Integer getId() {
        return id;
    }

    public ProduitPerime setId(Integer id) {
        this.id = id;
        return this;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public ProduitPerime setProduit(@NotNull Produit produit) {
        this.produit = produit;
        return this;
    }

    public Lot getLot() {
        return lot;
    }

    public ProduitPerime setLot(Lot lot) {
        this.lot = lot;
        return this;
    }

    public @NotNull LocalDateTime getCreated() {
        return created;
    }

    public ProduitPerime setCreated(@NotNull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Min(1)
    public int getQuantity() {
        return quantity;
    }

    public ProduitPerime setQuantity(@Min(1) int quantity) {
        this.quantity = quantity;
        return this;
    }

    public @NotNull LocalDate getPeremptionDate() {
        return peremptionDate;
    }

    public ProduitPerime setPeremptionDate(@NotNull LocalDate peremptionDate) {
        this.peremptionDate = peremptionDate;
        return this;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public ProduitPerime setUser(@NotNull AppUser user) {
        this.user = user;
        return this;
    }

    @Min(1)
    @NotNull
    public int getInitStock() {
        return initStock;
    }

    public ProduitPerime setInitStock(@Min(1) @NotNull int initStock) {
        this.initStock = initStock;
        return this;
    }

    @NotNull
    public int getAfterStock() {
        return afterStock;
    }

    public ProduitPerime setAfterStock(@NotNull int afterStock) {
        this.afterStock = afterStock;
        return this;
    }
}
