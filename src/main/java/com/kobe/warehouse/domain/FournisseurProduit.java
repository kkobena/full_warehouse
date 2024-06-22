package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A FournisseurProduit. */
@Entity
@Table(
    name = "fournisseur_produit",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"produit_id", "fournisseur_id"}),
      @UniqueConstraint(columnNames = {"code_cip", "fournisseur_id"})
    },
    indexes = {
      @Index(columnList = "code_cip ASC", name = "code_cip_index"),
      @Index(columnList = "principal", name = "principal_index")
    })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FournisseurProduit extends AbstractAuditingEntity implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  private Long id;


  @NotNull
  @Column(name = "code_cip", nullable = false)
  private String codeCip;


  @NotNull
  @Min(value = 1)
  @Column(name = "prix_achat", nullable = false)
  private Integer prixAchat;


  @NotNull
  @Min(value = 1)
  @Column(name = "prix_uni", nullable = false)
  private Integer prixUni;


  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();


  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;


  @NotNull
  @Column(name = "principal", nullable = false, columnDefinition = "boolean default false")
  private Boolean principal;


  @ManyToOne(optional = false)
  @NotNull
  @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
  private Produit produit;


  @ManyToOne(optional = false)
  @NotNull
  @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
  private Fournisseur fournisseur;


  @OneToMany(mappedBy = "fournisseurProduit")
  private Set<OrderLine> orderLines = new HashSet<>();

  @Override
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

    public @NotNull @Min(value = 1) Integer getPrixAchat() {
        return prixAchat;
    }

  public void setPrixAchat(Integer prixAchat) {
    this.prixAchat = prixAchat;
  }

    public @NotNull @Min(value = 1) Integer getPrixUni() {
        return prixUni;
    }

  public void setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
  }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

    public @NotNull Boolean getPrincipal() {
        return principal;
    }

  public void setPrincipal(Boolean principal) {
    this.principal = principal;
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

  public FournisseurProduit createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public FournisseurProduit updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Boolean isPrincipal() {
    return principal;
  }

  public FournisseurProduit principal(Boolean principal) {
    this.principal = principal;
    return this;
  }

  public FournisseurProduit produit(Produit produit) {
    this.produit = produit;
    return this;
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
    return "FournisseurProduit{"
        + "id="
        + getId()
        + ", codeCip='"
        + getCodeCip()
        + "'"
        + ", prixAchat="
        + getPrixAchat()
        + ", prixUni="
        + getPrixUni()
        + ", createdAt='"
        + getCreatedAt()
        + "'"
        + ", updatedAt='"
        + getUpdatedAt()
        + "'"
        + ", principal='"
        + isPrincipal()
        + "'"
        + "}";
  }
}
