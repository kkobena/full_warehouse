package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "ligne_avoir",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"avoir_id", "produit_id"})})
public class LigneAvoir implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "qte", nullable = false)
  private Integer quantite;

  @NotNull
  @Column(name = "qte_servi", nullable = false)
  private Integer quantiteServi = 0;

  @ManyToOne(optional = false)
  @NotNull
  @JsonIgnoreProperties(value = "ligneAvoirs", allowSetters = true)
  private Avoir avoir;

  @NotNull
  @ManyToOne(optional = false)
  private Produit produit;

  public Long getId() {
    return id;
  }

  public LigneAvoir setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getQuantite() {
    return quantite;
  }

  public LigneAvoir setQuantite(Integer quantite) {
    this.quantite = quantite;
    return this;
  }

  public Integer getQuantiteServi() {
    return quantiteServi;
  }

  public LigneAvoir setQuantiteServi(Integer quantiteServi) {
    this.quantiteServi = quantiteServi;
    return this;
  }

  public Avoir getAvoir() {
    return avoir;
  }

  public LigneAvoir setAvoir(Avoir avoir) {
    this.avoir = avoir;
    return this;
  }

  public Produit getProduit() {
    return produit;
  }

  public LigneAvoir setProduit(Produit produit) {
    this.produit = produit;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LigneAvoir that = (LigneAvoir) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
