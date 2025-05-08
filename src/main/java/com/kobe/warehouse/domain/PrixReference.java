package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "produit_tiers_payant_prix",  uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id" ,"enabled"}) ,@UniqueConstraint(columnNames = { "produit_id", "tiers_payant_id" ,"prix_type"})})
public class PrixReference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Min(value = 1)
    private int valeur;
    @NotNull
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private TiersPayant tiersPayant;
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Produit produit;
    private boolean enabled= true;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "prix_type", nullable = false)
    private PrixReferenceType type;
    private LocalDateTime created = LocalDateTime.now();
    private LocalDateTime updated = LocalDateTime.now();
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
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

    public PrixReference setId(Long id) {
        this.id = id;
        return this;
    }


    public TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public PrixReference setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public PrixReference setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PrixReference setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public PrixReferenceType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PrixReference that = (PrixReference) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public PrixReference setType(PrixReferenceType type) {
        this.type = type;
        return this;
    }

    public int getValeur() {
        return valeur;
    }

    public void setValeur(int valeur) {
        this.valeur = valeur;
    }

    public float getTaux() {
        if (type==PrixReferenceType.POURCENTAGE) {
            return valeur / 100.0f;
        }
        return  0.0f;
    }
}
