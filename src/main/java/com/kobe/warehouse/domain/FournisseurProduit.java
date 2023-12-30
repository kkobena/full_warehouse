package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
import lombok.Getter;
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

  @Getter
  @NotNull
  @Column(name = "code_cip", nullable = false)
  private String codeCip;

  @Getter
  @NotNull
  @Min(value = 1)
  @Column(name = "prix_achat", nullable = false)
  private Integer prixAchat;

  @Getter
  @NotNull
  @Min(value = 1)
  @Column(name = "prix_uni", nullable = false)
  private Integer prixUni;

  @Getter
  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Getter
  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Getter
  @NotNull
  @Column(name = "principal", nullable = false, columnDefinition = "boolean default false")
  private Boolean principal;

  @Getter
  @ManyToOne(optional = false)
  @NotNull
  @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
  private Produit produit;

  @Getter
  @ManyToOne(optional = false)
  @NotNull
  @JsonIgnoreProperties(value = "fournisseurProduits", allowSetters = true)
  private Fournisseur fournisseur;

  @Getter
  @OneToMany(mappedBy = "fournisseurProduit")
  private Set<OrderLine> orderLines = new HashSet<>();

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public FournisseurProduit setOrderLines(Set<OrderLine> orderLines) {
    this.orderLines = orderLines;
    return this;
  }

  public void setCodeCip(String codeCip) {
    this.codeCip = codeCip;
  }

  public FournisseurProduit codeCip(String codeCip) {
    this.codeCip = codeCip;
    return this;
  }

  public void setPrixAchat(Integer prixAchat) {
    this.prixAchat = prixAchat;
  }

  public FournisseurProduit prixAchat(Integer prixAchat) {
    this.prixAchat = prixAchat;
    return this;
  }

  public void setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
  }

  public FournisseurProduit prixUni(Integer prixUni) {
    this.prixUni = prixUni;
    return this;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public FournisseurProduit createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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

  public void setProduit(Produit produit) {
    this.produit = produit;
  }

  public FournisseurProduit produit(Produit produit) {
    this.produit = produit;
    return this;
  }

  public void setPrincipal(Boolean principal) {
    this.principal = principal;
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
