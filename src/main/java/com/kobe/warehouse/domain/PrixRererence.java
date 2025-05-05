package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "produit_tiers_payant_prix",  uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id" ,"enabled"}) ,@UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id" ,"prix_type"})})
public class PrixRererence implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Min(value = 1)
    private int prix;
    @NotNull
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private TiersPayant tiersPayant;
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Produit produit;
    private boolean enabled= true;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "prix_type", nullable = false)
    private PrixRererenceType type;

    public int prix() {
        return prix;
    }

    public void setPrix(int prix) {
        this.prix = prix;
    }

    public Long getId() {
        return id;
    }

    public PrixRererence setId(Long id) {
        this.id = id;
        return this;
    }

    public int getPrix() {
        return prix;
    }

    public TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public PrixRererence setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public PrixRererence setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PrixRererence setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public PrixRererenceType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PrixRererence that = (PrixRererence) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public PrixRererence setType(PrixRererenceType type) {
        this.type = type;
        return this;
    }

    public float getTaux() {
        return  prix / 100.0f;
    }
}
