package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "product_state",
    indexes = { @Index(columnList = "state", name = "state_index") },
    uniqueConstraints = { @UniqueConstraint(columnNames = { "state", "produit_id" }) }
)
public class ProductState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime updated = LocalDateTime.now();

    @ManyToOne(optional = false)
    @NotNull
    private Produit produit;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "state", nullable = false, length = 1)
    private ProductStateEnum state;

    public Long getId() {
        return id;
    }

    public ProductState setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProductState setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public ProductState setProduit(@NotNull Produit produit) {
        this.produit = produit;
        return this;
    }

    public @NotNull ProductStateEnum getState() {
        return state;
    }

    public ProductState setState(@NotNull ProductStateEnum state) {
        this.state = state;
        return this;
    }
}
