package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A FournisseurProduit.
 */
@Entity
@Table(
    name = "fournisseur_produit",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "produit_id", "fournisseur_id" }),
        @UniqueConstraint(columnNames = { "code_cip", "fournisseur_id" }),
    },
    indexes = { @Index(columnList = "code_cip ASC", name = "code_cip_index") ,@Index(columnList = "code_ean", name = "code_ean_index"),}
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FournisseurProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_cip")
    private String codeCip;

    @Column(name = "code_ean")
    private String codeEan;

    @NotNull
    @Column(name = "prix_achat", nullable = false)
    private Integer prixAchat;

    @NotNull
    @Column(name = "prix_uni", nullable = false)
    private Integer prixUni;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
    private Produit produit;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
    private Fournisseur fournisseur;

    @OneToMany(mappedBy = "fournisseurProduit", fetch = FetchType.LAZY)
    private Set<OrderLine> orderLines = new HashSet<>();

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getCodeCip() {
        return codeCip;
    }

    public void setCodeCip(String codeCip) {
        this.codeCip = codeCip;
    }

    public @NotNull Integer getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public @NotNull Integer getPrixUni() {
        return prixUni;
    }

    public void setPrixUni(Integer prixUni) {
        this.prixUni = prixUni;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public Set<OrderLine> getOrderLines() {
        return orderLines;
    }

    public FournisseurProduit setOrderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
        return this;
    }

    public FournisseurProduit codeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public FournisseurProduit prixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public FournisseurProduit prixUni(Integer prixUni) {
        this.prixUni = prixUni;
        return this;
    }

    public FournisseurProduit produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCodeEan() {
        return codeEan;
    }

    public void setCodeEan(String codeEan) {
        this.codeEan = codeEan;
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
        return (
            "FournisseurProduit{" +
            "id=" +
            getId() +
            ", codeCip='" +
            getCodeCip() +
            "'" +
            ", prixAchat=" +
            getPrixAchat() +
            ", prixUni=" +
            getPrixUni() +
            ", createdAt='" +
            "'" +
            ", principal='" +
            "'" +
            "}"
        );
    }
}
