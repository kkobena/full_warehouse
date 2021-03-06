package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

/**
 * A FournisseurProduit.
 */
@Entity
@Table(name = "fournisseur_produit", uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id", "fournisseur_id"}),@UniqueConstraint(columnNames = {"code_cip", "fournisseur_id"})},    indexes = {
    @Index(columnList = "code_cip ASC", name = "code_cip_index"),
    @Index(columnList = "principal", name = "principal_index")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FournisseurProduit implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Column(name = "code_cip", nullable = false)
    private String codeCip;
    @NotNull
    @Min(value =1)
    @Column(name = "prix_achat", nullable = false)
    private Integer prixAchat;
    @NotNull
    @Min(value = 1)
    @Column(name = "prix_uni", nullable = false)
    private Integer prixUni;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt=Instant.now();
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @NotNull
    @Column(name = "principal", nullable = false,columnDefinition = "boolean default false")
    private Boolean principal;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
    private Fournisseur fournisseur;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public FournisseurProduit codeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public void setCodeCip(String codeCip) {
        this.codeCip = codeCip;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public FournisseurProduit prixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public Integer getPrixUni() {
        return prixUni;
    }

    public FournisseurProduit prixUni(Integer prixUni) {
        this.prixUni = prixUni;
        return this;
    }

    public void setPrixUni(Integer prixUni) {
        this.prixUni = prixUni;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public FournisseurProduit createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public FournisseurProduit updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean isPrincipal() {
        return principal;
    }

    public FournisseurProduit principal(Boolean principal) {
        this.principal = principal;
        return this;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public Produit getProduit() {
        return produit;
    }

    public FournisseurProduit produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FournisseurProduit)) {
            return false;
        }
        return id != null && id.equals(((FournisseurProduit) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }


    @Override
    public String toString() {
        return "FournisseurProduit{" +
            "id=" + getId() +
            ", codeCip='" + getCodeCip() + "'" +
            ", prixAchat=" + getPrixAchat() +
            ", prixUni=" + getPrixUni() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", principal='" + isPrincipal() + "'" +
            "}";
    }
}
