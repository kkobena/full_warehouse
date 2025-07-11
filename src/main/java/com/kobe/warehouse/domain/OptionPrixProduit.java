package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "produit_tiers_payant_prix",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id", "enabled" }),
        @UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id", "prix_type" }),
    }
)
public class OptionPrixProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int valeur;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private TiersPayant tiersPayant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produit produit;

    @ColumnDefault(value = "true")
    private boolean enabled = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prix_type", nullable = false)
    private OptionPrixType type;

    @ColumnDefault(value = "now()")
    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "updated", nullable = false)
    @ColumnDefault(value = "now()")
    private LocalDateTime updated = LocalDateTime.now();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Long getId() {
        return id;
    }

    public OptionPrixProduit setId(Long id) {
        this.id = id;
        return this;
    }

    public TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public OptionPrixProduit setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public OptionPrixProduit setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OptionPrixProduit setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public OptionPrixType getType() {
        return type;
    }

    public OptionPrixProduit setType(OptionPrixType type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OptionPrixProduit that = (OptionPrixProduit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public int getValeur() {
        return valeur;
    }

    public void setValeur(int valeur) {
        this.valeur = valeur;
    }

    public float getTaux() {
        if (type == OptionPrixType.POURCENTAGE) {
            return valeur / 100.0f;
        }
        return 0.0f;
    }
}
